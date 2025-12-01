import { CloudTasksClient } from "@google-cloud/tasks";
import {
    CollectionReference, DocumentData, Firestore, getFirestore, Timestamp,
} from "firebase-admin/firestore";
import { Change, DocumentSnapshot, onDocumentWritten } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { NotificationManager } from "./notificationManager";

/** location for Google Cloud tasks */
const taskLocation = "us-central1";

/** Name of the Google Cloud task queue used to call {drawLottery} when it is time */
const taskQueue = "firestore-lottery";

const debug = false;


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
    registrationEnd: Timestamp;
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

        if (debug) logger.debug("updateLottery", event.params.eventId);
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
        if (debug) logger.debug("refreshLotteries");
        await LotteryManager.getInstance().refreshLotteries();
    });

/**
* A function to respond to HTTP requests from Google Cloud tasks.
* This allows a task to draw a lottery at an exact time in the future.
*
* @type CloudFunction
*/
export const lotteryDrawCallback =
    onRequest(async (req, res) => {
        if (debug) logger.debug("lotteryDrawCallback");
        try {
            await LotteryManager.getInstance().drawLottery(req.body.eventId, false);
            res.status(200);
        } catch (err) {
            throw new HttpsError("internal", String(err));
        }
    });

/**
 * A function to draw a lottery early.
 *
 * @type CloudFunction
 */
export const drawLotteryEarly =
    onCall(async (req) => {
        if (debug) logger.debug("drawLotteryEarly");
        try {
            await LotteryManager.getInstance().drawLottery(req.data.lotteryID, true);
        } catch (err) {
            throw new HttpsError("internal", String(err));
        }
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
    eventsRef: CollectionReference;
    invitationsRef: CollectionReference;


    /**
     * Creates an instance of LotteryManager.
     *
     * @constructor
     */
    constructor() {
        this.tasksClient = new CloudTasksClient();
        this.db = getFirestore();
        this.eventsRef = this.db.collection("events");
        this.invitationsRef = this.db.collection("invitations");
    }

    /**
     * Updates Google Cloud tasks for an event that has been created, updated, or deleted.
     *
     * @param {Change<DocumentSnapshot>} data the change in the event document
     */
    async handleEventChange(data: Change<DocumentSnapshot>) {
        if (!data.after.exists) {
            if (debug) logger.debug("event deleted");
            // event deleted

            await this.deleteLotteryTask(data.before);
        } else if (!data.before.exists) {
            if (debug) logger.debug("event created");
            // event created

            await this.queueLotteryTask(data.after);
        } else {
            const timeBefore = data.before.get("registrationEnd") as Timestamp;
            const timeAfter = data.after.get("registrationEnd") as Timestamp;

            if (timeBefore == null || timeAfter == null) {
                logger.warn(`Ignoring ${data.after.id} since "registrationEnd" is null`);
                return;
            }

            if (!timeBefore.isEqual(timeAfter)) {
                if (debug) logger.debug("event updated");
                // event lottery time updated

                await this.deleteLotteryTask(data.after);
                await this.queueLotteryTask(data.after);
            } else {
                if (debug) logger.debug("event not updated");
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
        if (debug) logger.debug("refreshLotteries");

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
        if (debug) logger.debug("queueLotteryTask A", snap.id);
        const event = snap.data() as EventData;

        // don't reschedule a lottery
        const oldTaskName = event.lotteryTaskName;
        if (debug) logger.debug("queueLotteryTask B", event.lotteryComplete, oldTaskName);
        if (event.lotteryComplete || oldTaskName != null) return;

        const millisUntilLottery = event.registrationEnd.toMillis() - Date.now();
        if (debug) logger.debug("queueLotteryTask C", millisUntilLottery);
        if (millisUntilLottery >= taskMillisLimit) return;

        const payload: LotteryTaskPayload = {
            eventId: snap.id,
        };

        if (millisUntilLottery <= 0) {
            // no need to schedule, draw it now
            this.drawLottery(payload.eventId, false);
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
        const [{ name: taskName }] = await this.tasksClient.createTask({ parent: queuePath, task });
        if (debug) logger.debug("queueLotteryTask D", taskName);

        await snap.ref.update({ lotteryTaskName: taskName });
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
        if (debug) logger.debug("deleteLotteryTask", snap.id);
        const event = snap.data() as EventData;

        const task = event.lotteryTaskName;

        if (task != null) {
            await this.tasksClient.deleteTask({ name: task });
            await snap.ref.update({ lotteryTaskName: null });
        }
    }


    /**
     * Draws random users from the waitlist, and create invites for them.
     * Updates everything in the database accordingly.
     *
     * @async
     * @param {LotteryTaskPayload} eventId A payload from when the Google Cloud Task was first made
     * @param {LotteryTaskPayload} early If lottery is early it needs the Google Cloud Task deleted
     * @return {*}
     */
    async drawLottery(eventId: string, early: boolean) {
        const eventRef = this.eventsRef.doc(eventId);
        const eventSnap = await eventRef.get();

        // get event info
        const maxAttendees = eventSnap.get("maxAttendees") ?? Infinity as number;
        const waitingList = eventSnap.get("waitingList") as string[];
        const organizerId = eventSnap.get("organizerID") as string;
        const invites = eventSnap.get("invites") ?? [] as string[];

        if (waitingList == null) {
            logger.warn(`Ignoring ${eventRef.id} since "waitingList" is null`);
            return;
        } else if (organizerId == null) {
            logger.warn(`Ignoring ${eventRef.id} since "organizerId" is null`);
            return;
        }

        const invitesIds = [];
        const winnerIds = [];
        const inviteCount = Math.min(maxAttendees - invites.length, waitingList.length);

        const tasks: Promise<unknown>[] = [];

        for (let i = 0; i < inviteCount; i++) {
            const inviteRef = this.invitationsRef.doc();
            invitesIds.push(inviteRef.id);

            // pick random index and remove from waitingList
            const index = Math.floor(Math.random() * waitingList.length);
            const [recipientId] = waitingList.splice(index, 1);

            winnerIds.push(recipientId);

            // create invite
            tasks.push(inviteRef.create({
                accepted: false,
                cancelTime: null,
                cancelled: false,
                event: eventSnap.id,
                invitation: inviteRef.id,
                organizerID: organizerId,
                recipientID: recipientId,
                responseTime: null,
                sendTime: Timestamp.now(),
            }));
        }

        // update event
        tasks.push(eventRef.set({
            "lotteryComplete": true,
            "invites": invitesIds,
            "waitingList": waitingList,
        }, {
            merge: true,
        }));

        if (early) {
            tasks.push(this.deleteLotteryTask(eventSnap));
        }

        const eventName = eventSnap.get("name");
        const notificationManager = NotificationManager.getInstance();
        tasks.push(
            notificationManager.notifyOfLottery(eventId, eventName, winnerIds, waitingList));

        // wait for everything to finish
        await Promise.all(tasks);
    }
}
