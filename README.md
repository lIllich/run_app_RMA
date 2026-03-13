# run_app_RMA
RunApp is a team project from the college class 'Mobile Apps Development'. Its main purpose is to track a user's GPS and accelerometer data while running (or walking) and post it as a social post where followers can like or comment. Users can also follow other users, and like or comment on their posts.

## Tech Stack
- Android Studio (Jetpack Compose)
- Room – internal Android DB for relational data about runs (GPS and accelerometer data)
- Google Firebase – for storing users' data, posts, followers, comments, likes, ...
- Google Firebase Authentication – for user authentication
- Google Functions – for push notifications about posts, comments, followers, likes, ...

## App Layout and Functionalities
The app is divided into sections that can be accessed by tab labels placed at the bottom of the screen. Here are all tabs and their main functionalities.

### Feed Tab
The Feed tab shows all posts from followed users. It contains "Post" cards with relevant information about the post (username and user profile photo, timestamp, like button, and information about the run: distance, pace, and duration). Tapping a "Post" card opens the Post screen where all information is shown and it is also possible to comment. Two buttons are also accessible here: "Map", which shows a screen with the route on Google Maps API, and "ElevationGraph", which shows a screen with an elevation graph over time.

### Search Tab
The Search tab's main purpose is to find users by name and start following them in order to see their posts in the Feed tab.

### Running Tab
The Running tab has only 2 buttons: "Start Run" and "End Run". It tracks GPS and accelerometer data and saves it to RoomDB.

### Post Tab
The main purpose of this tab is to see all runs saved to the device and be able to choose which run the user wants to post to their profile.

### Challenges Tab
The Challenges tab contains 5 static, hard-coded challenges that a new user can accomplish. It is more of a concept that could be implemented in more depth.

### Profile Tab
Shows the user's data (profile photo, email, age, total distance, number of runs, ...) and multiple buttons:
- to edit profile data,
- to log out,
- to see the user's posts,
- to see followed users,
- to see followers.
