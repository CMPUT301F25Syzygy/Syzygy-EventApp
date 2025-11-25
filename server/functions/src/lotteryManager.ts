import { CloudTasksClient } from "@google-cloud/tasks";
import { DocumentData, Firestore, getFirestore, Timestamp } from "firebase-admin/firestore";
import { Change, DocumentSnapshot, onDocumentWritten } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onRequest } from "firebase-functions/v2/https";

/** location for Google Cloud tasks */
const taskLocation = "us-central1";

/** Name of the Google Cloud task queue used to call {drawLottery} when it is time */
const taskQueue = "firestore-lottery";


/**
 * Google Cloud tasks can't be scheduled more than 30 days out.
 * Lotteries farther out than {taskMillisLimit} are not scheduled as tasks immediately.
 * Instead they wait until they are periodically checked later.
 */
const taskMillisLimit = 1000 * 60 * 60 * 24 * 10; // 10 days in milliseconds

/**
 * How often to run {refreshLotteries} to keep all tasks running correctly.
 * Must not be farther apart that {taskMillisLimit}.
 */
const refreshLotteriesSchedule = "0 0 * * 0"; // every Sunday at midnight

interface EventData extends DocumentData {
    registrationEnd: Timestamp
    lotteryComplete: boolean;
    lotteryTaskName: string | null | undefined;
}

interface LotteryTaskPayload {
    eventId: string;
}

/**
 * Updates the {LotteryManager} when an event is created, updated, or deleted.
 *
 * @type CloudFunction
 */
export const updateLottery =
    onDocumentWritten("/events/{eventId}", async (event) => {
        if (event === undefined || event.data === undefined) return;

        logger.debug("updateLottery", event.params.eventId);
        await LotteryManager.getInstance().handleEventChange(event.data);
    });

/**
 * Makes the {LotteryManager} periodically refresh old events to create Google Cloud tasks for them.
 * This is important since Google Cloud tasks have their duration limited at 30 days.
 * Events created with registration end dates far future end dates would otherwise be forgotten.
 *
 * @type CloudFunction
 */
export const refreshLotteries =
    onSchedule(refreshLotteriesSchedule, async () => {
        logger.debug("refreshLotteries");
        await LotteryManager.getInstance().refreshLotteries();
    });

/**
* A function to respond to HTTP requests from Google Cloud tasks.
* This allows a task to draw a lottery at an exact time in the future.
*
* @type CloudFunction
*/
export const lotteryDrawCallback =
    onRequest(async (req) => {
        logger.debug("lotteryDrawCallback");
        await LotteryManager.getInstance().drawLottery(req.body);
    });


/**
 * A singleton that handles drawing event lotteries.
 * It can create Google Cloud tasks to draw them later, without any need for frequent checking.
 */
class LotteryManager {
    static singletonInstance: LotteryManager;

    /**
     * Gets the {LotteryManager} singleton, creating it if needed.
     *
     * @static
     * @return {LotteryManager} the {LotteryManager} singleton
     */
    static getInstance(): LotteryManager {
        if (this.singletonInstance === undefined) {
            this.singletonInstance = new LotteryManager();
        }

        return this.singletonInstance;
    }

    tasksClient: CloudTasksClient;
    db: Firestore;


    /**
     * Creates an instance of LotteryManager.
     *
     * @constructor
     */
    constructor() {
        this.tasksClient = new CloudTasksClient();
        this.db = getFirestore();
    }

    /**
     * Updates Google Cloud tasks for an event that has been created, updated, or deleted.
     *
     * @param {Change<DocumentSnapshot>} data the change in the event document
     */
    async handleEventChange(data: Change<DocumentSnapshot>) {
        if (!data.after.exists) {
            logger.debug("event deleted");
            // event deleted

            await this.deleteLotteryTask(data.before);
        } else if (!data.before.exists) {
            logger.debug("event created");
            // event created

            await this.queueLotteryTask(data.after);
        } else {
            const timeBefore = data.before.get("registrationEnd") as Timestamp;
            const timeAfter = data.after.get("registrationEnd") as Timestamp;

            if (!timeBefore.isEqual(timeAfter)) {
                logger.debug("event updated");
                // event lottery time updated

                await this.deleteLotteryTask(data.after);
                await this.queueLotteryTask(data.after);
            } else {
                logger.debug("event not updated");
            }
        }
    }


    /**
     * Checks if any old events should have Google Cloud tasks created for them.
     *
     * @async
     * @return {*}
     */
    async refreshLotteries() {
        logger.debug("refreshLotteries");

        const snap = await this.db.collection("events").get();

        for (const eventSnap of snap.docs) {
            this.queueLotteryTask(eventSnap);
        }
    }

    /**
    * Creates a Google Cloud task to call {lotteryDrawCallback} for an event.
    * This will draw a lottery when an event reaches the end of it's registration period.
    *
    * Does not create a task if one already exists or the lottery is complete.
    * Does not create a task if it is more than {taskSecondLimit} seconds in the future.W
    * Draws a lottery immediately if the registration period has ended without being drawn.
    *
    * @async
    * @param {DocumentSnapshot} snap a snapshot of the event document
    * @return {*}
    */
    async queueLotteryTask(snap: DocumentSnapshot) {
        logger.debug("queueLotteryTask", snap.id);
        const event = snap.data() as EventData;

        // don't reschedule a lottery
        logger.debug("queueLotteryTask2", event.lotteryComplete, event.lotteryTaskName);
        if (event.lotteryComplete || event.lotteryTaskName != null) return;

        const millisUntilLottery = event.registrationEnd.toMillis() - Date.now();
        logger.debug("queueLotteryTask3", millisUntilLottery);
        if (millisUntilLottery >= taskMillisLimit) return;

        const payload: LotteryTaskPayload = {
            eventId: snap.id,
        };

        if (millisUntilLottery <= 0) {
            // no need to schedule, draw it now
            this.drawLottery(payload);
            return;
        }

        const firebaseConfig = process.env.FIREBASE_CONFIG;
        if (firebaseConfig === undefined) {
            const message = "FIREBASE_CONFIG environment variable not found";
            logger.error(message);
            throw Error(message);
        }

        const projectId = JSON.parse(firebaseConfig).projectId as string;
        const url = `https://${taskLocation}-${projectId}.cloudfunctions.net/lotteryDrawCallback`;

        const task = {
            httpRequest: {
                httpMethod: "POST" as const,
                url,
                body: Buffer.from(JSON.stringify(payload)).toString("base64"),
                headers: {
                    "Content-Type": "application/json",
                },
            },
            scheduleTime: event.registrationEnd,
        };

        const queuePath = this.tasksClient.queuePath(projectId, taskLocation, taskQueue);
        const [{ name }] = await this.tasksClient.createTask({ parent: queuePath, task });
        logger.debug("queueLotteryTask4", name);

        await snap.ref.update({ lotteryTaskName: name });
    }

    /**
     * Deletes a Google Cloud task that has already been created.
     * This prevents the event lottery from being drawn at the predefined time.
     *
     * @async
     * @param {DocumentSnapshot} snap a snapshot of the event document
     * @return {*}
     */
    async deleteLotteryTask(snap: DocumentSnapshot) {
        logger.debug("deleteLotteryTask", snap.id);
        const event = snap.data() as EventData;

        const task = event.lotteryTaskName;

        if (task !== undefined) {
            await this.tasksClient.deleteTask({ name: task });
            await snap.ref.update({ lotteryTaskName: null });
        }
    }


    /**
     * Draws random users from the waitlist, and create invites for them.
     * Updates everything in the database accordingly.
     *
     * @async
     * @param {LotteryTaskPayload} payload A payload from when the Google Cloud Task was first made
     * @return {*}
     */
    async drawLottery(payload: LotteryTaskPayload) {
        logger.debug("drawLottery", payload.eventId);
        const eventsCollection = this.db.collection("events");
        // const snap =
        await eventsCollection.doc(payload.eventId);
    }
}
