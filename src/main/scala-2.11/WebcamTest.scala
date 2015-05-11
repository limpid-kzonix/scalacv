import javax.swing.JFrame

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import com.github.sarxos.webcam.WebcamResolution

object WebcamTest extends App {

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

}