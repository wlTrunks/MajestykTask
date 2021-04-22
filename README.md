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

## Write unit test
 -ref some tests to a new feature
 -add new tests

## Allow the user to search for tasks in the list screen
 -add search task feature
 -made a simple approach to delay search input
 -add unit test

## Show empty view when tasks are empty; prompt users check network connection
 -add on lost connections alert dialog
 -add empty view

## Ref to coroutines, add DI
 -add dagger
 -migrate to coroutines
 well needed rewrite unit test, change livedata to MVI and stateflow example on TaskDetailViewModel
