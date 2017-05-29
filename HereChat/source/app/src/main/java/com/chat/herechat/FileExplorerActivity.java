package com.chat.herechat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;

import com.chat.herechat.Utilities.FilesDisplayer;
import com.chat.herechat.Utilities.FileItems;

public class FileExplorerActivity extends ListActivity{
	
	private File currentDir;
	private String rootDirPath;
	private FilesDisplayer adapter;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_picker);
		
		listView = (ListView) findViewById(android.R.id.list);
		
		currentDir = Environment.getExternalStorageDirectory();
		rootDirPath = currentDir.getName();
		fillDirectory(currentDir);
	}
	
	public void fillDirectory(File file){
		//Set title to the current directory
		setTitle("File Picker");
		
		//Retrieve all files in this directory
		File[] dirs = file.listFiles();		
		
		//List of directories
		List<FileItems> directories = new ArrayList<FileItems>();
		//List if files
		List<FileItems> files = new ArrayList<FileItems>();
		
		for(File f : dirs){
			//Is a directory
			if(f.isDirectory()){
				File[] innerFiles = f.listFiles();
				int numItems;
				if(innerFiles!=null)
					numItems = innerFiles.length;
				else
					numItems = 0;
				
				FileItems item = new FileItems(FileItems.DIRECTORY, f.getName(), numItems, f.getAbsolutePath());
				directories.add(item);
			}
			//Is a file
			else{
				FileItems item = new FileItems(FileItems.FILE, f.getName(), f.length(), f.getAbsolutePath());
				files.add(item);
			}
		}
		
		directories.addAll(files);
		
		if(!currentDir.getName().equals(rootDirPath)){
			directories.add(0, new FileItems(FileItems.UP, "../", file.getParent()));
		}
		
		adapter = new FilesDisplayer(this, directories);
		listView.setAdapter(adapter);		
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		FileItems item = (FileItems) adapter.getItem(position);
		
		int typeItem = item.getTypeItem();
		if(typeItem== FileItems.DIRECTORY || typeItem== FileItems.UP){
			currentDir = new File(item.getAbsolutePath());
			fillDirectory(currentDir);
		}
		else{
			chooseFile(item);
		}
	}
	
	public void chooseFile(final FileItems item){
		AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
		newDialog.setTitle("Send file");
		newDialog.setMessage("Do you wish to send this file?");
		
		newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = getIntent();
				intent.putExtra("filePath", item.getAbsolutePath());
				setResult(RESULT_OK, intent);
				finish();
			}
			
		});
		
		newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		newDialog.show();
	}
	
	@Override
	public void onBackPressed() {
		if(!currentDir.getName().equals(rootDirPath)){
			fillDirectory(currentDir.getParentFile());
			currentDir = currentDir.getParentFile();
		}
		else{
			finish();
		}
	}
}