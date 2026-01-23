package net.capsule.update;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.capsule.update.util.UpdateManager;
import net.capsule.update.util.Util;
import net.capsule.update.util.VersionChecker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.JButton;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class UpdateFrame extends JFrame implements ActionListener {
	private static final File capsuleExecLocation = new File(Util.getDirectory() + "jars/Capsule.jar");
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JProgressBar bar;
	private JLabel statusText;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		VersionChecker.initVersionChecker();
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					
					UpdateFrame frame = new UpdateFrame();
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
					
					UpdateManager um = UpdateManager.instance;
					um.downloadCapsuleAndLibs();
					um.installAndRunUpdate((dp) -> {
						frame.bar.setIndeterminate(false);
						frame.bar.setValue(dp.percent());
						frame.statusText.setText(dp.progressName() + " - " + dp.toString());
						
						if (dp.isFinished()) {
							frame.dispose();
							try {
								VersionChecker.saveUsingLatestVersion();
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							frame.startCapsule(um, args);
						}
					}, (crash) -> {
						crash.printStackTrace();
						JOptionPane.showMessageDialog(frame, "Update Error: " + crash.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						frame.dispose();
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void startCapsule(UpdateManager manager, String[] args) {
		try {
			String sep = File.pathSeparator; // Windows ;  Linux :
			
			StringBuilder cpBuilder = new StringBuilder();
			cpBuilder.append(capsuleExecLocation.getAbsolutePath());
			for (File files : manager.getCapsuleLibs()) {
				cpBuilder.append(sep + files.getAbsolutePath());
			}
			
			List<String> arg = new ArrayList<>();
			arg.add("java");
			arg.add("-cp");
			arg.add(cpBuilder.toString());
			arg.add("net.capsule.Capsule");
			
			for (String s : args) {
				arg.add(s);
			}
			
			System.out.println(java.util.Arrays.toString(arg.toArray(new String[0])));
			
			Process p = Runtime.getRuntime().exec(arg.toArray(new String[0]));
			// Get the error stream
            InputStreamReader isr = new InputStreamReader(p.getErrorStream());
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Error: " + line);
            }

            // Wait for the process to finish
            int exitCode = p.waitFor();
            System.out.println("Exit Code: " + exitCode);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Launch Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			dispose();
		}
	}

	/**
	 * Create the frame.
	 */
	public UpdateFrame() {
		setUndecorated(true);
		setResizable(false);
		setTitle("Capsule Launcher");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 650, 397);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JPanel downPanel = new JPanel();
		downPanel.setBackground(new Color(230, 230, 230));
		contentPane.add(downPanel, BorderLayout.SOUTH);
		downPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		bar = new JProgressBar();
		bar.setForeground(new Color(0, 149, 0));
		bar.setStringPainted(true);
		bar.setIndeterminate(true);
		bar.setFont(new Font("Tahoma", Font.BOLD, 17));
		bar.setPreferredSize(new Dimension(500,30));
		downPanel.add(bar);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setPreferredSize(new Dimension(100, 33));
		downPanel.add(cancelButton);
		
		JPanel panel = new ImagePanel();
		contentPane.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));
		
		statusText = new JLabel("Installing Capsule");
		statusText.setOpaque(true);
		statusText.setBackground(new Color(148, 148, 148));
		statusText.setFont(new Font("Comic Sans MS", Font.BOLD, 16));
		statusText.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(statusText, BorderLayout.SOUTH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int a = JOptionPane.showConfirmDialog(UpdateFrame.this, "Are you sure you want to cancel?", "Capsule Launcher", JOptionPane.YES_NO_OPTION);
		if (a == JOptionPane.YES_OPTION) {
			UpdateFrame.this.dispose();
			System.exit(0);
		}
	}
}
