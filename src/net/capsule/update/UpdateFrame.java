package net.capsule.update;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.capsule.Version;
import net.capsule.update.util.UpdateManager;
import net.capsule.update.util.Util;
import net.capsule.update.util.VersionChecker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Desktop;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class UpdateFrame extends JFrame implements ActionListener {
	public static final Version capsuleLauncherVersion = new Version("0.3.1");
	
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
		
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				UpdateFrame frame = new UpdateFrame();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				
				// 2 saniye sonra devam et
			    new javax.swing.Timer(2000, e -> {
			        ((javax.swing.Timer) e.getSource()).stop();
			        startLauncherLogic(frame, args);
			    }).start();
			    
			} catch (Exception e) {
				e.printStackTrace();
				
				StringWriter writer = new StringWriter();
				e.printStackTrace(new PrintWriter(writer));
				
				JOptionPane.showMessageDialog(null, "Update Error: " + writer.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}
	
	private static void startLauncherLogic(UpdateFrame frame, String[] args) {
		UpdateManager um = UpdateManager.instance;
		um.downloadCapsuleAndLibs();
		
		new Thread(() ->{
			if (um.capsuleLauncherUpdateIsAvailable()) {
				frame.statusText.setText("Capsule Launcher has an update. " + capsuleLauncherVersion.toString() + " -> " + um.getLatestLauncherVersion());
				
				// TODO: Cross-platform support
				File launcherFile = new File(Util.getDirectory() + "CapsuleSetup.exe");
				try {
					launcherFile = File.createTempFile("new_launcher_", ".exe");
					Util.downloadFile(URI.create("https://github.com/Ramazanenescik04/Capsule-Launcher/releases/download/" + um.getLatestLauncherVersion() + "/CapsuleSetup.exe"),
							launcherFile,
							dp -> {
								frame.bar.setIndeterminate(false);
								frame.bar.setValue(dp.percent());
								frame.statusText.setText("Loading Launcher - " + dp.toString());
								
								if (dp.isFinished()) {
									frame.dispose();
								}
							});
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					
					if (Desktop.isDesktopSupported()) {
					    try {
					        Desktop.getDesktop().browse(URI.create("https://github.com/Ramazanenescik04/Capsule-Launcher/releases/latest"));
					    } catch (IOException ex) {
					    	ex.printStackTrace();
					    }
					    System.exit(1);
					}
				} finally {
					launcherFile.deleteOnExit();
					
					try {
						Process p = Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", "\"\"", launcherFile.getAbsolutePath()});
						p.waitFor();
						System.exit(0);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
				return;
			}
			
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
				
				System.exit(1);
			});
		}).start();;
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
			arg.add("--enable-native-access=ALL-UNNAMED");
			arg.add("-cp");
			arg.add(cpBuilder.toString());
			arg.add("net.capsule.Capsule");
			
			if (args.length > 0 && args[0].startsWith("capsule://")) {
				ParsedCapsule list = parseCPH(args[0]);
				arg.add(list.type);
				arg.add(list.id);
			} else {
				for (String s : args) {
					arg.add(s);
				}
			}
			
			Process p = Runtime.getRuntime().exec(arg.toArray(new String[0]));
			// Get the error stream
            InputStreamReader isr = new InputStreamReader(p.getErrorStream());
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                System.err.println(line);
            }

            // Wait for the process to finish
            int exitCode = p.waitFor();
            System.out.println("Exit Code: " + exitCode);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Launch Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			dispose();
			
			System.exit(1);
		}
	}
	
	private static ParsedCapsule parseCPH(String cph) {
		URI uri = URI.create(cph);

        if (!"capsule".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Geçersiz protocol: " + uri.getScheme());
        }

        String type = uri.getHost(); // game, user, vs.

        String path = uri.getPath(); // /12345
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("ID bulunamadı");
        }

        String id = path.substring(1); // baştaki '/' kaldır
        
        String capsuleType;
        if (type.equals("open")) {
        	capsuleType = "-game";
        } else if (type.equals("studio")) {
        	capsuleType = "-studio";
        } else {
        	capsuleType = "-game";
        }
        
        return new ParsedCapsule(capsuleType, id);
	}
	
	public record ParsedCapsule(String type, String id) {}

	/**
	 * Create the frame.
	 */
	public UpdateFrame() {
		setUndecorated(true);
		setResizable(false);
		setTitle("Capsule Launcher");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 633, 397);
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
		bar.setPreferredSize(new Dimension(600,30));
		downPanel.add(bar);
		bar.addNotify();
		
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
	}
}
