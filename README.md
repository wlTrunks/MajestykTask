# Android-Arch-Sample

## Display tasks in a list with the following design
So for the first taks:
 -add recycler view
 -add viewbinding feature

## Launch details screen when a task is clicked with the following design
 -add navigation component to navigate between fragments
 -changed getTaskById() return Single so error may happen and loader
 -add toolbar and as design with search and back images
 I don't really like a lot of livedata for error and loading...not needed gonna ref it later

## Change the app to always show cached/local data first, then show updates from API
-.Inform the user if unable to update from API; show a Snackbar with retry button.
 -refactored TasksRepositoryImpl getTasks and getTask method
 -add global RxJavaPlugins errorHandler
 -add snackbar show on network response error

## Allow tasks to be opened via deep link
 -add deep link feature
