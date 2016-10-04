package tw.com.mycompany.maptest;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.zip.Inflater;

public class MyDialog extends DialogFragment{
    MyMarker mMyMarker=null;

    public MyDialog setMarker(MyMarker myMarker)
    {
        mMyMarker = myMarker;
        return this;
    }

    public class MissionUpdateRunnable implements Runnable{
        int mId;
        int mDisp;
        public MissionUpdateRunnable(int id, int disp)
        {
            mId = id;
            mDisp = disp;
        }
        public void run() {
            String result = DBHelper.updateMission(mId, mDisp);
            Log.i(MapsActivity.TAG,result);
        }
    };


    @Override
    public Dialog onCreateDialog(Bundle savedInstance)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.info_content,null);
        View loading = (View) content.findViewById(R.id.loading_panel);
        String reporter="";
        String reason="";
        String cellphone="";
        boolean needCompany=false;
        boolean needReply=false;
        if (mMyMarker != null && mMyMarker.hasData()) {
            loading.setVisibility(View.GONE);
            reporter=mMyMarker.getReporter();
            reason=mMyMarker.getReason();
            cellphone=mMyMarker.getCellphone();
            needCompany=mMyMarker.getNeedCompany();
            needReply = mMyMarker.getNeedReply();
        }else
        {
            View tableView = (View) content.findViewById(R.id.dialog_table);
            tableView.setVisibility(View.GONE);
        }
        TextView viewReporter = (TextView) content.findViewById(R.id.reporter);
        TextView viewReason = (TextView) content.findViewById(R.id.reason);
        TextView viewCellphone = (TextView) content.findViewById(R.id.cellphone);
        CheckBox checkBoxNeedCompany = (CheckBox) content.findViewById(R.id.need_company_checkbox);
        CheckBox checkBoxNeedReply = (CheckBox) content.findViewById(R.id.need_reply_checkbox);

        viewReporter.setText(reporter);
        viewReason.setText(reason);
        viewCellphone.setText(cellphone);
        checkBoxNeedCompany.setChecked(needCompany);
        checkBoxNeedReply.setChecked(needReply);


        builder.setView(content)
                .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        if (mMyMarker.hasData()) {
            if (mMyMarker.getType() == MyMarker.TYPE.ATTENDING) {
                builder.setPositiveButton(getString(R.string.stop_mission), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyMarker.setType(mMyMarker.getType().next()).setHasNoData();
                        mMyMarker.remove();
                        Thread thread = new Thread(new MissionUpdateRunnable(mMyMarker.getId(), 1));
                        thread.start();
                    }
                }).setNegativeButton(getString(R.string.cancel_mission),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMyMarker.setType(mMyMarker.getType().prev()).setHasNoData();
                                ((MapsActivity)getActivity()).getMap().missionDeselected();
                                Thread thread = new Thread(new MissionUpdateRunnable(mMyMarker.getId(), -1));
                                thread.start();
                            }
                        });
            } else if (mMyMarker.getType() == MyMarker.TYPE.UNATTENDED){
                builder.setPositiveButton(getString(R.string.start_mission), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyMarker.setType(mMyMarker.getType().next()).setHasNoData();
                        ((MapsActivity)getActivity()).getMap().missionSelected(mMyMarker);
                        Thread thread = new Thread(new MissionUpdateRunnable(mMyMarker.getId(), 1));
                        thread.start();
                    }
                });
            }
        }
        return builder.create();
    }
}
