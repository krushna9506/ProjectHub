package com.projecthub.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.projecthub.models.Milestone;
import com.projecthub.models.Project;
import com.projecthub.models.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DeadlineReminderWorker extends Worker {

    private static final long WINDOW_MS = TimeUnit.HOURS.toMillis(48);

    public DeadlineReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!AppPreferences.isDeadlineRemindersEnabled(getApplicationContext())) {
            return Result.success();
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return Result.success();
        }
        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long now = System.currentTimeMillis();
        long cutoff = now + WINDOW_MS;
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayKey = dayFmt.format(new Date(now));

        try {
            QuerySnapshot projectSnaps = Tasks.await(
                db.collection("projects")
                    .whereGreaterThan("members." + uid, "")
                    .get(),
                20, TimeUnit.SECONDS
            );

            for (DocumentSnapshot projectDoc : projectSnaps.getDocuments()) {
                Project project = projectDoc.toObject(Project.class);
                if (project == null) continue;
                String projectId = projectDoc.getId();
                String projectTitle = project.getTitle() != null ? project.getTitle() : "Project";

                if (project.getDeadline() > now && project.getDeadline() <= cutoff) {
                    String key = "deadline_proj_" + projectId + "_" + todayKey;
                    if (NotificationHelper.shouldNotifyOnce(getApplicationContext(), key)) {
                        NotificationHelper.dispatchNotification(
                            getApplicationContext(),
                            uid,
                            "Project Deadline Approaching",
                            projectTitle + " is due within 48 hours.",
                            "DEADLINE",
                            projectId,
                            projectTitle
                        );
                    }
                }

                QuerySnapshot taskSnaps = Tasks.await(
                    db.collection("projects").document(projectId).collection("tasks").get(),
                    15, TimeUnit.SECONDS
                );
                for (DocumentSnapshot taskDoc : taskSnaps.getDocuments()) {
                    Task task = taskDoc.toObject(Task.class);
                    if (task == null) continue;
                    if ("DONE".equalsIgnoreCase(task.getStatus())) continue;

                    long due = task.getDueDate();
                    if (due > now && due <= cutoff) {
                        boolean relevant = uid.equals(task.getAssignedTo()) || uid.equals(project.getLeadId());
                        if (!relevant) continue;

                        String key = "deadline_task_" + taskDoc.getId() + "_" + todayKey;
                        if (NotificationHelper.shouldNotifyOnce(getApplicationContext(), key)) {
                            NotificationHelper.dispatchNotification(
                                getApplicationContext(),
                                uid,
                                "Task Due Soon: " + task.getTitle(),
                                "Due within 48h in " + projectTitle,
                                "DEADLINE",
                                projectId,
                                projectTitle
                            );
                        }
                    }
                }

                QuerySnapshot milestoneSnaps = Tasks.await(
                    db.collection("projects").document(projectId).collection("milestones").get(),
                    15, TimeUnit.SECONDS
                );
                for (DocumentSnapshot mDoc : milestoneSnaps.getDocuments()) {
                    Milestone milestone = mDoc.toObject(Milestone.class);
                    if (milestone == null) continue;
                    if ("COMPLETED".equalsIgnoreCase(milestone.getStatus()) || "DONE".equalsIgnoreCase(milestone.getStatus())) {
                        continue;
                    }
                    long due = milestone.getDueDate();
                    if (due > now && due <= cutoff) {
                        String key = "deadline_ms_" + mDoc.getId() + "_" + todayKey;
                        if (NotificationHelper.shouldNotifyOnce(getApplicationContext(), key)) {
                            NotificationHelper.dispatchNotification(
                                getApplicationContext(),
                                uid,
                                "Milestone Due Soon: " + milestone.getTitle(),
                                "Target date is within 48h in " + projectTitle,
                                "DEADLINE",
                                projectId,
                                projectTitle
                            );
                        }
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
