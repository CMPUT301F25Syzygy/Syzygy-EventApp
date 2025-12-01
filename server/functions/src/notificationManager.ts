import {
    CollectionReference, DocumentData, Firestore, getFirestore, Timestamp,
} from "firebase-admin/firestore";
import { logger } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { getMessaging, Messaging } from "firebase-admin/messaging";
import { onDocumentWritten } from "firebase-functions/v2/firestore";

interface Notification {
    id: number;
    eventId: string | null | undefined;
    organizerId: string | null | undefined;
    title: string;
    description: string;
    creationDate: Timestamp;
    sent: boolean;
    deleted: boolean;
}

interface UserNotification {
    userId: string,
    notificationId: number,
    sent: boolean;
}

interface NotificationData extends DocumentData, Notification { }

const debug = true;
const MAX_NOTIFICATION_ID = 2 ** 31 - 1;

/**
 * Sends notifications to users when they are added to the database
 *
 * @type CloudFunction
 */
export const sendNotificationFromDB =
    onDocumentWritten("/notifications/{notificationId}", async (event) => {
        if (event === undefined || event.data === undefined) return;
        const notificationId = Number(event.params.notificationId);

        // don't try if notification was just created
        if (!event.data.after.exists) return;

        if (debug) logger.debug("sendNotificationFromDB", notificationId);

        const notificationManager = NotificationManager.getInstance();
        const notif = await notificationManager.getNotification(notificationId);

        if (notif == null) {
            logger.error("Tried to load from database and failed", notificationId);
            return;
        }

        // don't send again
        if (notif.content.deleted) {
            await notificationManager.deleteNotification(
                notif.content.id,
                notif.recipientIds);
        } else if (!notif.content.sent) {
            // send to users
            await notificationManager.sendNotification(
                notif.content.id,
                notif.content.title,
                notif.content.description,
                notif.recipientIds,
                notif.content.eventId,
                notif.content.organizerId != null);

            // mark as sent
            await notificationManager.markNotificationSend(notificationId);
        }
    });

/**
 * A function to create a notification in the DB, and deploy it to users
 *
 * @type CloudFunction
 */
export const createNotification =
    onCall(async (req) => {
        if (debug) logger.debug("createNotification");
        try {
            const notificationManager = NotificationManager.getInstance();
            await notificationManager.createNotification(
                req.data.title,
                req.data.description,
                req.data.recipientIds,
                req.data.eventId,
                req.data.organizerId
            );
        } catch (err) {
            throw new HttpsError("internal", String(err));
        }
    });


/**
* A singleton that handles creating and sending notifications.
*/
export class NotificationManager {
    static singletonInstance: NotificationManager;

    /**
     * Gets the {NotificationManager} singleton, creating it if needed.
     *
     * @static
     * @return {NotificationManager} the {NotificationManager} singleton
     */
    static getInstance(): NotificationManager {
        if (this.singletonInstance === undefined) {
            this.singletonInstance = new NotificationManager();
        }

        return this.singletonInstance;
    }

    db: Firestore;
    messaging: Messaging;
    usersRef: CollectionReference;
    notifsRef: CollectionReference;
    userNotifsRef: CollectionReference;


    /**
     * Creates an instance of NotificationManager.
     *
     * @constructor
     */
    constructor() {
        this.db = getFirestore();
        this.messaging = getMessaging();
        this.usersRef = this.db.collection("users");
        this.notifsRef = this.db.collection("notifications");
        this.userNotifsRef = this.db.collection("userNotifications");
    }

    /**
     * Notifies users in the waiting list about a lottery that took place.
     *
     * @param {string} eventId id of the event
     * @param {string} eventName name of the event
     * @param {string[]} winnerIds ids of each user that won
     * @param {string[]} loserIds ids of each user that lost
     */
    async notifyOfLottery(
        eventId: string, eventName: string, winnerIds: string[], loserIds: string[]) {
        if (debug) logger.debug("notifyOfLottery", eventName, winnerIds, loserIds);

        const tasks: Promise<unknown>[] = [];
        tasks.push(this.createNotification(
            "Won event lottery",
            `You were selected to attend ${eventName}!`,
            winnerIds, eventId, null));

        tasks.push(this.createNotification(
            "Lost event lottery",
            `A lottery was run for ${eventName}. You were not selected this time.`,
            loserIds, eventId, null));

        await Promise.all(tasks);
    }

    /**
     * Creates a notification in the database and calls {sendNotification()} to send it to users.
     *
     * @param {string} title title of the notification
     * @param {string} description description of the notification
     * @param {string[]} recipientIds userIds of the recipients
     * @param {string | undefined} eventId associated event, optional
     * @param {string | undefined} organizerId associated organizer, optional
     */
    async createNotification(
        title: string, description: string, recipientIds: string[],
        eventId: string | null | undefined, organizerId: string | null | undefined) {
        if (recipientIds.length == 0) return;

        const id = Math.floor(Math.random() * MAX_NOTIFICATION_ID);
        const sendTask =
            this.sendNotification(
                id, title, description, recipientIds, eventId, organizerId != null);

        const notifRef = this.notifsRef.doc(String(id));

        const batch = this.db.batch();

        for (const recipientId of recipientIds) {
            const newUserNotif = this.userNotifsRef.doc();

            batch.create(newUserNotif, {
                userId: recipientId,
                notificationId: id,
                sent: false,
            } as UserNotification);
        }

        await batch.commit();

        await notifRef.create({
            id,
            eventId,
            organizerId,
            title,
            description,
            creationDate: Timestamp.now(),
            sent: true,
            deleted: false,
        } as Notification);

        await sendTask;
    }

    /**
     * Sends a notification to user devices.
     *
     * @param {number} id id of the notification
     * @param {string} title title of the notification
     * @param {string} description description of the notification
     * @param {string[]} recipientIds userIds of the recipients
     * @param {string | undefined} eventId associated eventId, optional
     * @param {boolean} fromOrganizer if the notification should be considered from an organizer
     */
    async sendNotification(
        id: number, title: string, description: string,
        recipientIds: string[], eventId: string | null | undefined, fromOrganizer: boolean) {
        if (recipientIds.length == 0) return;

        if (debug) logger.debug("sendNotification A", title, description, recipientIds, eventId);

        const recipientRefs = recipientIds.map(
            (recipientId) => this.usersRef.doc(recipientId));
        const recipientTokens: string[] = [];

        await this.db.getAll(...recipientRefs, {
            fieldMask: ["fcmToken", "organizerNotifications", "systemNotifications"],
        }).then(docs => {
            for (const [i, doc] of docs.entries()) {
                const token = doc.get("fcmToken");

                let enabled: boolean;
                if (fromOrganizer) {
                    enabled = doc.get("organizerNotifications") ?? true;
                } else {
                    enabled = doc.get("systemNotifications") ?? true;
                }

                if (token != null && enabled) {
                    this.userNotifsRef
                        .where("userId", "==", recipientIds[i])
                        .where("notificationId", "==", id).get().then((querySnap) => {
                            querySnap.forEach((snap) => {
                                snap.ref.set({
                                    "sent": true,
                                }, {
                                    merge: true,
                                });
                            });
                        });
                    recipientTokens.push(token);
                }
            }
        });

        const message = {
            data: {
                id: String(id),
                eventId: eventId ?? "",
                deleted: String(false),
            },
            notification: {
                title: title,
                body: description,
            },
            android: {
                collapse_key: String(id),
                notification: {
                    title: title,
                    body: description,
                    tag: String(id),
                },
            },
            tokens: recipientTokens,
        };

        if (debug) logger.debug("sendNotification B", JSON.stringify(message));

        if (recipientTokens.length > 0) {
            const response = await this.messaging.sendEachForMulticast(message);

            if (response.successCount == 0) {
                logger.error("Failed to send message to any recipients");
            } else if (response.failureCount > 0) {
                logger.warn("Failed to send to some recipients");
            }

            if (debug) logger.debug("sendNotification C", JSON.stringify(response));
        } else {
            if (debug) logger.debug("sendNotification C", "No recipientTokens");
        }
    }

    /**
     * Delete a notification already send user devices.
     *
     * @param {number} id id of the notification
     * @param {string[]} recipientIds userIds of the recipients
     */
    async deleteNotification(
        id: number, recipientIds: string[]) {
        if (recipientIds.length == 0) return;

        if (debug) logger.debug("deleteNotification A", recipientIds);

        const recipientRefs = recipientIds.map(
            (recipientId) => this.usersRef.doc(recipientId));
        const recipientTokens: string[] = [];

        await this.db.getAll(...recipientRefs, { fieldMask: ["fcmToken", "sent"] }).then(docs => {
            for (const doc of docs) {
                const token = doc.get("fcmToken");
                const wasSent = doc.get("sent") ?? true;

                if (token != null && wasSent) {
                    recipientTokens.push(token);
                }
            }
        });

        const message = {
            data: {
                id: String(id),
                deleted: String(true),
            },
            android: {
                collapse_key: String(id),
                notification: {
                    title: "Deleted",
                    body: "Deleted notification",
                    tag: String(id),
                },
            },
            tokens: recipientTokens,
        };

        if (debug) logger.debug("deleteNotification B", JSON.stringify(message));

        if (recipientTokens.length > 0) {
            const response = await this.messaging.sendEachForMulticast(message);

            if (response.successCount == 0) {
                logger.error("Failed to send message to any recipients");
            } else if (response.failureCount > 0) {
                logger.warn("Failed to send to some recipients");
            }

            if (debug) logger.debug("deleteNotification C", JSON.stringify(response));
        } else {
            if (debug) logger.debug("deleteNotification C", "No recipientTokens");
        }
    }

    /**
     * Gets a notification and it's recipientIds from the database
     *
     * @param {string} notificationId the id of the notification
     * @return {Promise<null | { content: NotificationData, recipientIds: string[] }>}
     *      the notification content, and recipients
     */
    async getNotification(notificationId: number):
        Promise<null | { content: NotificationData, recipientIds: string[] }> {
        const notifTask = this.notifsRef.doc(String(notificationId)).get();

        const notifUsersTask = await this.userNotifsRef
            .where("notificationId", "==", notificationId)
            .get();

        const notif = (await notifTask).data();

        if (notif == null) {
            return null;
        }

        const recipientIds = (await notifUsersTask).docs.map(
            (doc) => doc.get("userId")
        );

        return {
            content: notif as NotificationData,
            recipientIds,
        };
    }

    /**
     * Marks a notification as sent in the database
     *
     * @param {string} notificationId the id of the notification
     */
    async markNotificationSend(notificationId: number) {
        await this.notifsRef.doc(String(notificationId))
            .set({ "sent": true }, {
                merge: true,
            });
    }
}
