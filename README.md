# scalacv
Scala wrapper around the OpenCV3.00 Java API

The FaceDetect test app:
* rewrites the [Introduction to Java Development](http://docs.opencv.org/3.0-last-rst/doc/tutorials/introduction/desktop_java/java_dev_intro.html) OpenCV tutorial
* adds ideas from the FaceDetector app in https://github.com/chimpler/blog-scala-javacv
* introduces `Future`(s) to add an initial layer of concurrency

The CamFaceDetect test app:
* applies FaceDetect to a webcamera and is heavily indebted with https://github.com/rladstaetter/isight-java
