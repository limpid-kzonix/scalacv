# scalacv
Scala wrapper around the OpenCV3.00 Java API

The [FaceDetect](https://github.com/mcallisto/scalacv/blob/master/src/main/scala-2.11/it/callisto/scalacv/FaceDetect.scala) test app:
* rewrites the [Introduction to Java Development](http://docs.opencv.org/3.0-last-rst/doc/tutorials/introduction/desktop_java/java_dev_intro.html) OpenCV tutorial
* adds ideas from the FaceDetector app in https://github.com/chimpler/blog-scala-javacv
* introduces `Future`(s) to add an initial layer of concurrency

The [CamFaceDetect](https://github.com/mcallisto/scalacv/blob/master/src/main/scala-2.11/it/callisto/scalacv/CamFaceDetect.scala) test app:
* applies FaceDetect to a webcamera and is heavily indebted with https://github.com/rladstaetter/isight-java
* adds eyes detection too
