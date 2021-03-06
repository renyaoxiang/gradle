/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection.rules;

import com.google.common.collect.ImmutableMap;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot;
import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot.ChangeFilter;
import org.gradle.api.internal.changedetection.state.OutputFilesCollectionSnapshotter;
import org.gradle.api.internal.changedetection.state.TaskExecution;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class OutputFilesTaskStateChanges extends AbstractNamedFileSnapshotTaskStateChanges {
    private static final EnumSet<ChangeFilter> IGNORE_ADDED_FILES = EnumSet.of(ChangeFilter.IgnoreAddedFiles);

    public OutputFilesTaskStateChanges(TaskExecution previous, TaskExecution current, TaskInternal task, OutputFilesCollectionSnapshotter snapshotter) {
        super(task.getName(), previous, current, snapshotter, false, "Output", task.getOutputs().getFileProperties());
    }

    @Override
    public Map<String, FileCollectionSnapshot> getPrevious() {
        return previous.getOutputFilesSnapshot();
    }

    @Override
    protected Set<ChangeFilter> getFileChangeFilters() {
        return IGNORE_ADDED_FILES;
    }

    @Override
    public void saveCurrent() {
        final Map<String, FileCollectionSnapshot> outputFilesAfter = buildSnapshots(getTaskName(), getSnapshotter(), getTitle(), getFileProperties(), isAllowSnapshotReuse());

        ImmutableMap.Builder<String, FileCollectionSnapshot> builder = ImmutableMap.builder();
        for (Map.Entry<String, FileCollection> entry : fileProperties.entrySet()) {
            String propertyName = entry.getKey();
            FileCollection roots = entry.getValue();
            FileCollectionSnapshot beforeExecution = getCurrent().get(propertyName);
            FileCollectionSnapshot afterExecution = outputFilesAfter.get(propertyName);
            FileCollectionSnapshot afterPreviousExecution = getSnapshotAfterPreviousExecution(propertyName);
            FileCollectionSnapshot outputSnapshot = getSnapshotter().createOutputSnapshot(afterPreviousExecution, beforeExecution, afterExecution, roots);
            builder.put(propertyName, outputSnapshot);
        }

        current.setOutputFilesSnapshot(builder.build());
    }

    private FileCollectionSnapshot getSnapshotAfterPreviousExecution(String propertyName) {
        if (previous != null) {
            Map<String, FileCollectionSnapshot> previousSnapshots = previous.getOutputFilesSnapshot();
            if (previousSnapshots != null) {
                FileCollectionSnapshot afterPreviousExecution = previousSnapshots.get(propertyName);
                if (afterPreviousExecution != null) {
                    return afterPreviousExecution;
                }
            }
        }
        return getSnapshotter().emptySnapshot();
    }

    @Override
    protected OutputFilesCollectionSnapshotter getSnapshotter() {
        return (OutputFilesCollectionSnapshotter) super.getSnapshotter();
    }
}
