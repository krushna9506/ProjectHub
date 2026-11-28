const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Automatically delivers a background FCM push notification whenever
 * an in-app notification document is created under users/{userId}/notifications/{notificationId}.
 */
exports.sendPushOnNotificationCreate = onDocumentCreated(
  "users/{userId}/notifications/{notificationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return null;

    const notif = snapshot.data() || {};
    const userId = event.params.userId;

    const userSnap = await db.collection("users").doc(userId).get();
    if (!userSnap.exists) return null;

    const userData = userSnap.data() || {};
    const fcmToken = userData.fcmToken;
    if (!fcmToken || typeof fcmToken !== "string" || fcmToken.trim() === "") {
      return null;
    }

    const title = notif.title || "ProjectHub Alert";
    const body = notif.message || "You have a new update in ProjectHub.";
    const projectId = notif.projectId || "";
    const type = notif.type || "TASK";

    const payload = {
      token: fcmToken,
      notification: {
        title,
        body,
      },
      data: {
        title,
        body,
        projectId,
        type,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "projecthub_channel",
          priority: "high",
        },
      },
    };

    try {
      await messaging.send(payload);
    } catch (err) {
      console.error("Error sending FCM notification to user", userId, err);
    }
    return null;
  }
);

/**
 * Scheduled job running every 12 hours to check approaching deadlines (within 48h)
 * for active projects, tasks, and milestones, writing notification docs (which trigger FCM).
 */
exports.scheduledDeadlineAlerts = onSchedule("every 12 hours", async () => {
  const now = Date.now();
  const cutoff = now + 48 * 60 * 60 * 1000;
  const dayKey = new Date(now).toISOString().slice(0, 10);

  const projectsSnap = await db.collection("projects").get();
  for (const projectDoc of projectsSnap.docs) {
    const project = projectDoc.data() || {};
    const projectId = projectDoc.id;
    const projectTitle = project.title || "Project";

    const tasksSnap = await projectDoc.ref.collection("tasks").get();
    for (const taskDoc of tasksSnap.docs) {
      const task = taskDoc.data() || {};
      if ((task.status || "").toUpperCase() === "DONE") continue;

      const due = Number(task.dueDate || 0);
      const targetUid = task.assignedTo || project.leadId;
      if (!targetUid || due <= now || due > cutoff) continue;

      const dedupId = `deadline_task_${taskDoc.id}_${dayKey}`;
      const notifRef = db
        .collection("users")
        .doc(targetUid)
        .collection("notifications")
        .doc(dedupId);

      const existing = await notifRef.get();
      if (!existing.exists) {
        await notifRef.set({
          notificationId: dedupId,
          userId: targetUid,
          title: `Task Due Soon: ${task.title || "Task"}`,
          message: `Due within 48 hours in ${projectTitle}`,
          type: "DEADLINE",
          projectId,
          projectTitle,
          timestamp: now,
          read: false,
        });
      }
    }

    const milestonesSnap = await projectDoc.ref.collection("milestones").get();
    for (const mDoc of milestonesSnap.docs) {
      const milestone = mDoc.data() || {};
      const status = (milestone.status || "").toUpperCase();
      if (status === "COMPLETED" || status === "DONE") continue;

      const due = Number(milestone.dueDate || 0);
      const targetUid = project.leadId;
      if (!targetUid || due <= now || due > cutoff) continue;

      const dedupId = `deadline_ms_${mDoc.id}_${dayKey}`;
      const notifRef = db
        .collection("users")
        .doc(targetUid)
        .collection("notifications")
        .doc(dedupId);

      const existing = await notifRef.get();
      if (!existing.exists) {
        await notifRef.set({
          notificationId: dedupId,
          userId: targetUid,
          title: `Milestone Due Soon: ${milestone.title || "Milestone"}`,
          message: `Target date is within 48 hours in ${projectTitle}`,
          type: "DEADLINE",
          projectId,
          projectTitle,
          timestamp: now,
          read: false,
        });
      }
    }
  }
  return null;
});
