package com.example.user.tgifire;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;

public class UserMainActivity extends AppCompatActivity {

    Context mContext = this;

    int currentFloor = 0;

    ListView listview;
    RelativeLayout mainView;

    // DB 관련
    int downloadCount = 0, floorIndex = 0;
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();
    FirebaseStorage storage = FirebaseStorage.getInstance("gs://tgifire-cdf25.appspot.com/");
    StorageReference storageReference = storage.getReference();
    private String nearestID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_admin_main);

        mainView = (RelativeLayout) findViewById(R.id.canvas_layout);
        currentFloor = 0;

        getNearestID();
    }

    private void InitializeUI() {
        String[] items = new String[Building.getInstance().floorNumber];
        for (int i = 0; i < Building.getInstance().floorNumber; i++) {
            items[i] = Integer.toString(i + 1) + "층";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_floor, items);
        listview = (ListView) findViewById(R.id.drawer_menulist);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                currentFloor = position;
                mainView.setBackgroundDrawable(FloorPicture.getInstance().floorPicture.get(currentFloor));
                drawNodes();
                Toast.makeText(mContext, Integer.toString(position+1), Toast.LENGTH_SHORT).show();

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                drawer.closeDrawer(Gravity.START) ;
            }
        });

        mainView.setBackgroundDrawable(FloorPicture.getInstance().floorPicture.get(currentFloor));
        drawNodes();

        // 노드 센서 정보를 DB에서 계속 수신
        databaseReference.child("BUILDING").child(nearestID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Building.class) != null) {
                    Building.getInstance().SetData(dataSnapshot.getValue(Building.class).buildingName,
                            dataSnapshot.getValue(Building.class).GPS_X,
                            dataSnapshot.getValue(Building.class).GPS_Y,
                            dataSnapshot.getValue(Building.class).address,
                            dataSnapshot.getValue(Building.class).floorNumber,
                            dataSnapshot.getValue(Building.class).nodes);
                    if (Building.getInstance().nodes == null) {
                        Building.getInstance().nodes = new ArrayList<Node>();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawNodes();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void drawNodes() {
        mainView.removeAllViewsInLayout();

        RelativeLayout.LayoutParams lp;

        // 신고 버튼
        Button plusButton = new Button(this); //버튼을 선언

        lp = new RelativeLayout.LayoutParams(120, 120);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lp.rightMargin = 50;
        lp.bottomMargin = 50;
        plusButton.setLayoutParams(lp);
        plusButton.setAlpha(0.9f);
        plusButton.setBackgroundResource(R.drawable.report); //버튼 이미지를 지정(int)
        plusButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                GPSLocation GPS = new GPSLocation(mContext);
                double GPS_X = GPS.getGPS_X();
                double GPS_Y = GPS.getGPS_Y();

                Intent reportActivityIntent = new Intent(mContext, ReportActivity.class);
                reportActivityIntent.putExtra("address", GPSLocation.getAddress(mContext, GPS_X, GPS_Y));
                startActivity(reportActivityIntent);
            }
        });
        mainView.addView(plusButton);

        if ("".equals(nearestID)) {
            return;
        }

        // 노드들
        for (int i = 0; i < Building.getInstance().nodes.size(); i++) {
            if (Building.getInstance().nodes.get(i).floor == currentFloor) {
                Button newNode = new Button(this); //버튼을 선언

                lp = new RelativeLayout.LayoutParams(120, 120);
                lp.leftMargin = Building.getInstance().nodes.get(i).x - 60;
                lp.topMargin = Building.getInstance().nodes.get(i).y - 60;
                newNode.setLayoutParams(lp);
                newNode.setAlpha(0.75f);
                if (Building.getInstance().nodes.get(i).state) {
                    newNode.setBackgroundResource(R.drawable.node_red); //버튼 이미지를 지정(int)
                }
                else {
                    newNode.setBackgroundResource(R.drawable.node_green); //버튼 이미지를 지정(int)
                }
                newNode.setTag(i);

                newNode.setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        AlertDialog.Builder mBuilder = new AlertDialog.Builder(UserMainActivity.this);
                        View mView = getLayoutInflater().inflate(R.layout.node_info_uneditable, null);
                        final TextView nodeName = (TextView) mView.findViewById(R.id.textNodeName);
                        final TextView nodeState = (TextView) mView.findViewById(R.id.textNodeState);
                        Button buttonUserNodeExit = (Button) mView.findViewById(R.id.buttonUserNodeExit);

                        nodeName.setText(Building.getInstance().nodes.get((int) v.getTag()).name);
                        if (Building.getInstance().nodes.get((int) v.getTag()).state)
                            nodeState.setText("불법 적재물이 감지되었습니다.");
                        else
                            nodeState.setText("불법 적재물이 없습니다.");

                        mBuilder.setView(mView);
                        final AlertDialog dialog = mBuilder.create();
                        dialog.show();

                        buttonUserNodeExit.setOnClickListener(new Button.OnClickListener() {
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                    }
                });

                mainView.addView(newNode); //지정된 뷰에 셋팅완료된 Button을 추가
            }
        }
    }

    private double getDistance(double x1, double y1, double x2, double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private void getNearestID() {
        databaseReference.child("BUILDING").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GPSLocation GPS = new GPSLocation(mContext);
                double GPS_X = GPS.getGPS_X();
                double GPS_Y = GPS.getGPS_Y();
                double min_distance = 1000000.0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    double gps_x = 0.0, gps_y = 0.0;
                    for (DataSnapshot s : snapshot.getChildren()) {
                        if ("GPS_X".equals(s.getKey())) {
                            gps_x = Double.parseDouble(s.getValue().toString());
                        }
                        if ("GPS_Y".equals(s.getKey())) {
                            gps_y = Double.parseDouble(s.getValue().toString());
                        }
                    }

                    if (min_distance > getDistance(GPS_X, GPS_Y, gps_x, gps_y)) {
                        min_distance = getDistance(GPS_X, GPS_Y, gps_x, gps_y);
                        nearestID = snapshot.getKey();
                    }
                }

                if (min_distance >= 1) {
                    nearestID = "";
                }

                loadItemsFromDBFirst();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void loadItemsFromDBFirst() {
        Building.getInstance().Initialize();
        FloorPicture.getInstance().Initialize();

        if ("".equals(nearestID)) {
            mainView.setBackgroundResource(R.drawable.error);
            drawNodes();
            return;
        }

        databaseReference.child("BUILDING").child(nearestID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue(Building.class) != null) {
                    Building.getInstance().SetData(dataSnapshot.getValue(Building.class).buildingName,
                            dataSnapshot.getValue(Building.class).GPS_X,
                            dataSnapshot.getValue(Building.class).GPS_Y,
                            dataSnapshot.getValue(Building.class).address,
                            dataSnapshot.getValue(Building.class).floorNumber,
                            dataSnapshot.getValue(Building.class).nodes);
                    if (Building.getInstance().nodes == null) {
                        Building.getInstance().nodes = new ArrayList<Node>();
                    }
                }

                else {
                    // 건물이 없습니다.
                    return;
                }

                // 층별 사진 다운로드
                for (floorIndex = 0; floorIndex < Building.getInstance().floorNumber; floorIndex++) {
                    FloorPicture.getInstance().floorPicture.add(new BitmapDrawable());
                }
                downloadCount = 0;
                for (floorIndex = 0; floorIndex < Building.getInstance().floorNumber; floorIndex++) {
                    final long ONE_MEGABYTE = 20 * 1024 * 1024;
                    StorageReference spaceReference = storageReference.child(nearestID + "/floor" + Integer.toString(floorIndex + 1) + ".png");
                    spaceReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        final int index = floorIndex;
                        @Override
                        public void onSuccess(byte[] bytes) {
                            FloorPicture.getInstance().floorPicture.set(index, new BitmapDrawable(BitmapFactory.decodeByteArray(bytes, 0, bytes.length)));
                            downloadCount++;
                            if (downloadCount == Building.getInstance().floorNumber) {
                                InitializeUI();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}