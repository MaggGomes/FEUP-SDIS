package com.chat.herechat.FileTransfer;

/**
 * Created by almeida on 13/05/2017.
 */

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


public class FilePicker {


    public void openFile(String minmeType) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        Intent i = Intent.createChooser(intent, "File");
        //startActivityForResult(i, CHOOSE_FILE_REQUESTCODE);
    }
}
