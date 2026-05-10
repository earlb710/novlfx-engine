Added shared management-app working directory support with persisted last-folder selection and a browse control in the launcher.
Passed the selected working directory into the manual management screens so their initial browse/load locations stay aligned, including the reloadable JSON screen fallback path.
Updated the screen designer to show the active working directory above the navigation tree and added focused tests plus manual-doc updates for the new workflow.
Followed up by keeping the screen designer working directory fixed while loading or saving other screen JSON files instead of retargeting it to the loaded file location.
Updated preview rendering so relative screen resources resolve from the active working directory in both the designer preview and the reloadable JSON preview screen.
Moved the former screen-designer top-row preview/validation/default-value actions into an Edit menu and kept temporary-field actions available from the tree context menu instead.
Fixed preview rendering so screen-level `screenBackgroundImage` metadata now uses the active working directory and displays in designer/reloadable preview scenes instead of only block backgrounds.
