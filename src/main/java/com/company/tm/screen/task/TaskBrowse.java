package com.company.tm.screen.task;

import com.company.tm.app.TaskImportService;
import com.company.tm.entity.Task;
import io.jmix.core.DataManager;
import io.jmix.core.FileStorage;
import io.jmix.ui.Notifications;
import io.jmix.ui.Notifications.NotificationType;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.*;
import io.jmix.ui.download.Downloader;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.CollectionLoader;
import io.jmix.ui.screen.LookupComponent;
import io.jmix.ui.screen.*;
import io.jmix.ui.upload.TemporaryStorage;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@UiController("tm_Task.browse")
@UiDescriptor("task-browse.xml")
@LookupComponent("tasksTable")
public class TaskBrowse extends StandardLookup<Task> {

    @Autowired
    private CollectionLoader<Task> tasksDl;

    @Autowired
    private TaskImportService taskImportService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Downloader downloader;
    @Autowired
    private FileStorageUploadField importTasksField;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CollectionContainer<Task> tasksDc;


    @Subscribe("importBtn")
    public void onImportBtnClick(Button.ClickEvent event) {
        int tasks = taskImportService.importTasks();
        if (tasks > 0) {
            notifications.create()
                    .withCaption(tasks + " tasks imported")
                    .withType(NotificationType.TRAY)
                    .show();
        }

        tasksDl.load();
    }

    @Install(to = "tasksTable.attachment", subject = "columnGenerator")
    private Component tasksTableAttachmentColumnGenerator(final Task task) {
        if (task.getAttachment() == null) {
            return null;
        }

        LinkButton linkButton = uiComponents.create(LinkButton.class);

        linkButton.setCaption(task.getAttachment().getFileName());
        linkButton.addClickListener(e -> downloader.download(task.getAttachment()));

        return linkButton;
    }

    @Subscribe("importTasksField")
    public void onImportTasksFieldFileUploadSucceed(final SingleFileUploadField.FileUploadSucceedEvent event) throws IOException {
        UUID fileId = importTasksField.getFileId();
        if (fileId == null) {
            return;
        }

        File temporaryStorageFile = temporaryStorage.getFile(fileId);
        if (temporaryStorageFile != null) {
            process(temporaryStorageFile);
            temporaryStorage.deleteFile(fileId);
        }
    }

    private void process(File temporaryStorageFile) throws IOException {
        List<String> readLines = FileUtils.readLines(temporaryStorageFile, StandardCharsets.UTF_8);
        for (String line : readLines) {
            Task task = dataManager.create(Task.class);
            task.setName(line);

            tasksDc.getMutableItems().add(task);
        }
    }

}