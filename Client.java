import java.awt.Canvas;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Base64;

import javax.swing.*;

public class Client extends JFrame implements ActionListener{
	
	static JTextArea txt_aSend;
	static JTextArea txt_aReceive;
	static JFrame frame;
	static JButton btn1;
	static JButton btn2;
	static JLabel lbl1;
		
	
	static InputStream ins;
	static OutputStream outs;
	
	static InputStreamReader in;
	static OutputStreamWriter out;
	
	static DataOutputStream dout;
	static DataInputStream din;
	
	static BufferedReader bReader;
	static BufferedWriter bWriter;
	
	static Socket s;

	static String fullFolderPath;
	
	//default constructor 
	Client(){}
	
	
	//main
	public static void main(String[] args) {
		
		Client cl = new Client();
		
		String folderPath = System.getProperty("user.dir");
		String fullFolderPath = folderPath.concat("\\ReceivedPicC");
		File f1 = new File(fullFolderPath);
		f1.mkdir();  
	    
		
	    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	    //GUI
	    
		frame = new JFrame("CLIENT");
		
		JLabel l1 = new JLabel("Enter your message in this box: "); 
		l1.setBounds(10, 5, 220, 15);
		l1.setFont(new Font("Serif", Font.BOLD, 14));
		l1.setForeground(new Color(204, 255, 255));
		
		btn1 = new JButton("SEND");
		btn1.setBackground(new Color(204, 255, 255));
		btn1.addActionListener(cl);
		btn1.setBounds(380,30,90,100);
		btn1.setFont(new Font("Serif", Font.BOLD, 14));
		
		btn2 = new JButton( new AbstractAction("View received photos") {
	        @Override
	        public void actionPerformed( ActionEvent e ) {
	        	try {
	        			Desktop.getDesktop().open(new File(fullFolderPath));
	    		}catch(IOException e2) {
	    			e2.printStackTrace();
	    		}
	        }
	    });
		btn2.setBounds(65,410,350,40);
		btn2.setBackground(new Color(204, 255, 255));
		btn2.setFont(new Font("Serif", Font.BOLD, 14));
		
		JLabel l2 = new JLabel("RECEIVED: ");  
		l2.setBounds(50, 160, 200, 15);
		l2.setFont(new Font("Serif", Font.BOLD, 20));
		l2.setForeground(new Color(204, 255, 255));
	
		txt_aSend = new JTextArea("");
		txt_aSend.setBounds(10, 30, 345, 100);
		txt_aSend.setBackground(new Color(100, 193, 193));
		
		txt_aReceive = new JTextArea("Received:");  
		txt_aReceive.setEditable(false);
		txt_aReceive.setBackground(new Color(100, 193, 193));
		
		JScrollPane scroll = new JScrollPane (txt_aReceive, 
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setBounds(30,190,410,195);
		
		Canvas c = new Canvas();
	    frame.getContentPane().setBackground(new Color(0, 51, 51));
		frame.add(c);
		frame.add(txt_aSend);
		frame.add(l1);
		frame.add(l2);
		frame.add(scroll);
		frame.add(btn1);
		frame.add(btn2);
		frame.setLayout(null);
		frame.setSize(500, 500);
		frame.setResizable(false);
		frame.setVisible(true);
		
		
		
		
	
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// uspostavljanje konekcije, slanje i prijem 

		
		try {
			s = new Socket("localhost", 1115);

			ins = s.getInputStream();
			outs = s.getOutputStream();
			
			in = new InputStreamReader(ins);
			out = new OutputStreamWriter(outs);
				
			bReader = new BufferedReader(in);
			bWriter = new BufferedWriter(out);
			
			dout = new DataOutputStream(s.getOutputStream());
			din = new DataInputStream(s.getInputStream());
			
			
			
			
			///////////////////////////////////////////////////////////////////////////////////////////////////
			// PRIJEM
			
			Thread receive = new Thread(new Runnable() {
				
				String msg;
				String ident;  //ident moze biti TEXT ili PIC ili quit 
				String line;
				int pic_len;
				String pic_name;
				int numSend; 
				int i;
				
				@Override
				public void run() {
					try {
						ident = bReader.readLine();
						while(!ident.equals("quit")) {
							if(ident.equals("TEXT")) {
								//PRIJEM TEKSTUALNE PORUKE
								msg = bReader.readLine();
								msg = bReader.readLine();
								txt_aReceive.append("\nServer: " +  msg);
							}else if(ident.equals("PIC")){
								//PRIJEM SLIKE
								line = bReader.readLine();
								String lines[] = line.split("#");  //jer saljem drugu liniju u formatu: pic_name#pic_len
								pic_name = lines[0];
								pic_len = Integer.parseInt(lines[1]);
								File pic = new File(fullFolderPath + "\\" +  pic_name); //putanja do foldera u kom se cuvaju slike + ime slike 
								FileOutputStream fout = new FileOutputStream(pic);
								byte[] readData = new byte[1024];   
								numSend = 0;
								while(numSend != pic_len)    //numsend je trenutan broj bajta ucitanih i ispisanih 
			                    { 
									i = din.read(readData); //ucitavam po 1024 bajta (ili manje u poslednjem ucitavanju)
									numSend += i;
			                        fout.write(readData, 0, i);;  //pisem po 1024 bajta u novi fajl(sliku) 
			                    }
								fout.flush();
								fout.close();

								txt_aReceive.append("\nYou received a picture from Server ");
							}
							ident = bReader.readLine();
							
						}
						s.close();
						frame.setVisible(false);
						frame.dispose();
					}catch(IOException e) {
						e.printStackTrace();
					}
				}
			});	
			receive.start();
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	//SLANJE
	
	@Override 
	public void actionPerformed(ActionEvent e) {
		Thread sender = new Thread(new Runnable() {
			String msg;
			String pic_name;
			String ident;
			int pic_len;
			
			@Override
			public void run() {
				msg = txt_aSend.getText().trim();
				ident = msg.substring(0, 4);
				try {
					if(ident.equals("TEXT")) {
						//SLANJE TEKSTUALNE PORUKE
						bWriter.write(msg);
						bWriter.newLine();
						bWriter.flush();
						txt_aSend.setText("");
					}
					else if(ident.contains("PIC")){
						//SLANJE SLIKE
						int i;
						String lines[] = msg.split("\\r?\\n");
						pic_name = lines[1];
						
						try {
							File file = new File(pic_name);
							pic_len = (int)file.length();
							msg = msg.concat("#");
							msg = msg.concat(Integer.toString(pic_len));
							bWriter.write(msg);
							bWriter.newLine();
							bWriter.flush();
							FileInputStream fin = new FileInputStream(file);
							
							byte[] readData = new byte[1024];

		                    while((i = fin.read(readData)) != -1)
			                {
			                    dout.write(readData, 0, i);
			                }
		                    
			                fin.close();
			                txt_aSend.setText("");
						}catch(IOException e) {
							e.printStackTrace();
						}

					}else {
						msg = "quit";
						bWriter.write(msg);
						bWriter.newLine();
						bWriter.flush();
						frame.setVisible(false);
						frame.dispose();
						s.close();
					}
				}catch(IOException e){
					e.printStackTrace();
				}
			}	
		});
		sender.start();
	}
	
}