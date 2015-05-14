import javax.swing.JFrame

import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import com.github.sarxos.webcam.WebcamResolution

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.objdetect.CascadeClassifier

object WebcamFaceDetect extends App {

  def bufferedImageToMat(bi: BufferedImage): Mat = {
    val pixels = bi.getRaster().getDataBuffer.asInstanceOf[DataBufferByte].getData()
    val mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3)
    mat.put(0, 0, pixels)
    mat
  }
  
  System.load("/home/mario/dev/tools/opencv-3.0.0-rc1/build/lib/libopencv_java300.so")
  
  val webcam = Webcam.getDefault()
  webcam.setViewSize(WebcamResolution.VGA.getSize())

  val panel = new WebcamPanel(webcam)
  panel.setFPSDisplayed(true)
  panel.setDisplayDebugInfo(true)
  panel.setImageSizeDisplayed(true)
  panel.setMirrored(true)

  val window = new JFrame("Test webcam panel")
  window.add(panel)
  window.setResizable(true)
  window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  window.pack()
  window.setVisible(true)

  var n = 0
  while (true) {

    if (!webcam.isOpen()) {
      throw new Exception
    }

    val image = webcam.getImage()

    if (image == null) {
      throw new Exception
    }
    val mat = bufferedImageToMat(image)

//    if (n != 0 && n % 100 == 0) {
      println(Thread.currentThread().getName() + ": Frames captured: " + n)
//    }
    n = n + 1
    
  }

}