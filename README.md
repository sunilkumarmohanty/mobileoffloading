# **Mobile Cloud Computing Project**  
## **Assignment 2 (Group 7)**


------
#### Project structure

|folder            |description                                       |
|------------------|--------------------------------------------------|
|android           |the android client                                |
|web               |the web client                                    |
|server            |the backend server                                |
|mongo-k8s-sidecar |the opensource project used for mongo-replication |
|node_modules      |the dependencies resolved by npm (created by npm) |


------
#### Deployment
Run the deploy_script.sh file to start the deployment of backend server and the
web application. Please note that you may need to add execute permission for the 
deploy_script.sh file. Run `sudo chmod +x deploy_script.sh` to assign the 
permissions run `./deploy_script.sh` to execute the script.

The deploy_script.sh creates a cluster named "mean-cluster". The cluster contains 
2 nodes of machine type "n1-standard-2". The opensource project 
https://github.com/leportlabs/mongo-k8s-sidecar.git is used to perform mongoDB 
replication. Currently, 3 pods/replicas of mongoDB are created which are handled 
by 3 respective services. In case a mongo replica fails the respective service 
will autometically create a replacement replica. The web backend and web app are 
deployed under the same container, which is replicated over 2 replicas.
After the server deployment is complete, the deploy_script.sh invokes another 
script called "create_test_user.sh" which is responsible to create a demo user 
in the database. The demo user has the following credentials: 
username = "username" and password = "password".

Overall, we have 5 pods in total: 2 for web-app/backend and 3 for mongoDB

Finally, deploy_script.sh also runs android_deploy.sh script to build Android 
application and deploy it on device. android_deploy.sh script can be run 
separately from deploy_script.sh. Run `sudo chmod 777 android_deploy.sh` to 
assign the permissions and run `./android_deploy.sh` to execute the script.

------
#### Test credentials
Demo user:
username: "username"
password: "password"

------
#### Scan functionalities
The web client uses a JavaScript port of the Tesseract library called TesseractJS
which is developed by MIT [1]. The scan is using training data for the English
language.

The backend uses the same library as the web client, TesseractJS. There is an
issue with the library in case of NodeJS backends and JPEG images, and the
scan would be extremely slow. Thus the backend converts all JPEG images into
the PNG format before scanning. The backend returns processing times with
the scan results for each image.

Scanning at the backend has been adapted to synchronous serial processing from
asynchronous simultaneous processing to get more accurate per-image processing
time results. Otherwise the later images would show inaccurately long processing
times. This may lead to marginally longer total scan time but ensures that the
per-image results are sensible.

[1] http://tesseract.projectnaptha.com/

------
#### SSL
All traffic is forced through HTTPS instead of HTTP, and self-signed certificates
are used for this (so the web client should be expected to ask for a security
exception).

------
#### Authentication

The backend authenticates the necessary endpoints by using JSON Web Tokens.
A token is generated when a user logs in (or registers which is also an
  immediate login) and the client needs to include that token in all
  authenticated requests.

For normally registered new users, hashes of the passwords are stored in the
database (generated with the NodeJS crypto module [1]). On login, the backend
verifies matching hashes and provices the client an authentication token.

[1] https://nodejs.org/api/crypto.html

------
#### Facebook login
Facebook login is implemented by first using the Facebook Login API to make
a request from the client to Facebook to login the user in their service,
and if Facebook provides successfully an access token, the token is forwarded
with another request to the backend server which then verifies that token
with Facebook by using the Passport library [1] with a token strategy [2]
designed for the purpose of verifying the access token.

If the token is successfully verified, the backend server creates a new user
account if that user has never logged in. Once a user account exists, an
authentication token is generated as for a normal login with a User schema
method, and the token is sent to the client as if they logged in without an
external Identity Provider.

[1] http://passportjs.org/
[2] https://github.com/drudge/passport-facebook-token

------
#### Database
The database is a MongoDB database handled by the backend with the Mongoose
library [1]. User and scan data including references to filenames are stored
in normal Mongoose schemas, but files are stored using MongoDB GridFS. When a
request for a remote scan is made, the backend temporarily stores the images on
disk, makes a thumbnail of 200px width of the images, and then stores both the
originals and the thumbnails to the database with GridFS using streams [3].

A scan is performed using the locally temporarily stored images, and references
for filenames for the originals and thumbnails are stored into the Scan objects
so that they can be retrieved later on. After the local files are no longer needed
and everything is in the database, the local files are deleted.

[1] http://mongoosejs.com/
[2] https://docs.mongodb.com/v3.2/core/gridfs/
[3] https://www.npmjs.com/package/gridfs-stream

------
#### Cleanup of old original images
The backend runs a Cron job every day at 00:00 Finnish time to remove from the
database those original images that are over 7 days old. After that only
thumbnails can be shown in the clients for the related scan results. The NPM
module cron is used for this [1].

[1] https://www.npmjs.com/package/cron


------
#### Web client
The web client is implemented as a single page application using AngularJS.
It performs local OCR in the device but requires an internet connection for
proper functionality since it is a web page served by the backend. TesseractJS
also uses a CDN to store some of its internal JavaScript files, so an internet
connection is required for that reason as well, but all actual scan processing
is performed locally.

Note that due to the HTML5 API and file handling in file inputs in mobile devices,
the input designed to take in files from the file system / existing library does
not immediately launch the library since iOS and Android want to display their
own popup for the file source. However, the other input does lead directly to
the device camera since an attribute has been implemented in the HTML5 API for
that purpose (the `capture` attribute).

#### Android client
Android client is implemented using following libraries: Retrofit and Okhttp for 
remote API access, Gson for json serialization and deserialization, facebook-android-sdk
for facebook authentication, Slider and Glide for image viewing and tess-two for OCR.
Android app covers all requirements described in assignment. Challenge implementation 
is described below.

------
#### API endpoints

The authentication related endpoints return a token to be used with all
authenticated requests:

##### Register new user with `POST /api/register`

sample request data: `{"username":"username", "password":"password"}`,
content type is `Content-Type: application/json`

##### Login with `POST /api/login`

sample request data: `{"username":"username", "password":"password"}`,
content type is `Content-Type: application/json`

##### Login through Facebook with `POST /api/login/facebook`

sample request data: `{"access_token": "sometokenfromfacebook"}`,
content type is `Content-Type: application/json`

The following endpoints are authenticated with the token provided by the
login or register endpoints:

##### Remote scan with `POST /api/scan`

The request is files from e.g. a form with but always with content type `Content-Type: multi-part/formdata`

##### Scan history with `GET /api/history`

Returns the scan history for the logged in user

##### Get a file with `GET /api/:filename`

Returns from the MongoDB GridFS database the specified file

##### Delete a file with `GET /api/delete/:filename`

Deletes from the MongoDB GridFS database the specified file

##### Facebook login for Android App

Please add the hash key to the facebook application in developers.facebook.com. This hash can be checked when you try login using facebook for the first time.

##### Challenge : Offline for Android App

Android application checks the availability of network connection and reacts accordingly by offering the Local 
operating mode only. It also stores OCR history and thumbnails in cache (but not the original images). Without network
 connection the original images on OCR details screen will be empty. Application recognize network status only after restart.