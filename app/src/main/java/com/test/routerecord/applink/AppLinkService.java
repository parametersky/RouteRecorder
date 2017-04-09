package com.test.routerecord.applink;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.amap.api.maps2d.LocationSource;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.exception.SdlExceptionCause;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.AlertManeuverResponse;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GPSData;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleData;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.HMICapabilities;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PermissionItem;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SoftButton;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.UpdateTurnListResponse;
import com.smartdevicelink.proxy.rpc.enums.AudioStreamingState;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.LayoutMode;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.test.routerecord.MainActivity;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

//import android.util.Log;
/*
 * Main implementation of AppLink, responsible for:
 * 1. create/dispose SdlProxyALM, handle connection with Sdl
 * 2. building UI on Sdl, when get HMI_FULL first time, send show, addcommand, createchoiceset to Sdl.
 * 3. handling user action with Sdl and notification from Sdl.
 * 4. send lockscreen broadcast to MainActivity.
 * 5. update Music Player Service status on Sdl
 */
public class AppLinkService extends Service implements IProxyListenerALM {

    private static final int CMD_ID_MOSTPOPULAR = 1011;
    private static final int CMD_ID_FAVORITES = 1012;
    private static final int CMD_ID_PLAYLISTS = 1013;
    private static final int CMD_ID_LOCAL = 1014;
    private static final int CMD_ID_ADDFAVORITE = 1017;
    private static final int CMD_ID_SONGINFO = 1018;
    private static final int CMD_ID_NEWAGE = 1019;
    private static final int BTN_ID_PLAY = 1022;
    private static final int BTN_ID_PAUSE = 1025;
    private static final int BTN_ID_PLAYLISTS = 1024;
    private static final int BTN_ID_FAVORITE = 1313;
    private static final int BTN_ID_UNFAVORITE = 1316;
    private static final int CHS_ID_PLAYLISTS = 1041;
    private static AppLinkService instance = null;
    private static SdlProxyALM mSdlProxy = null;
    private final String TAG = "AppLinkService";
    private int correlationID = 100;
    private HMILevel hmilevel = null;

    private boolean getFirstRun = false;
    private boolean firstHMINone = true;

    private int putFileIconID = 22;
    private String iconFileName = "icon.png";
    private String mPlayerStatus = null;
    private String mCoverName = null;
    private boolean mPicToShow = false;
    private int putCoverID = 23;

    private boolean isGen3 = true;

    private LocationSource.OnLocationChangedListener listener;
    private Thread mWorkThread;

    private boolean working = false;

    private MainActivity activity = null;

    public void setMainActivity(MainActivity activity1){
        activity = activity1;
//        activity.setUpMap();
    }

    public void subscribeMusicButton() {
        try {
            mSdlProxy.subscribeButton(ButtonName.SEEKLEFT, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.SEEKRIGHT, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.OK, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_1, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_2, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_3, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_4, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_5, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_6, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_7, correlationID++);
            mSdlProxy.subscribeButton(ButtonName.PRESET_8, correlationID++);

        } catch (SdlException e) {
            // TODO Auto-generated catch onblock
            e.printStackTrace();
        }
    }

    public String getStringValue(int id) {
        return getResources().getString(id);
    }


    public void pump(String content, String text2) {
        try {
            mSdlProxy.alert(content, text2, false, 3000, correlationID++);
//            mSdlProxy.resumeMediaClockTimer();
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void voicePump(String content, String text2) {
        try {
            String voicestring = null;
            if (text2 == null) {
                voicestring = content;
            } else {
                voicestring = content + "," + text2;
            }
            mSdlProxy.alert(TTSChunkFactory.createSimpleTTSChunks(voicestring),
                    content, text2, false, 3000, correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void scrollableMessage(String message, Vector<SoftButton> softButtons) {
        try {
            mSdlProxy.scrollablemessage(message, 10000, softButtons,
                    correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void performInteraction(int choicesetid, String initprompt,
                                   String displaytext, InteractionMode mode) {
//            mSdlProxy.performInteraction(initprompt, displaytext, choicesetid,
//                    null, null, mode, 10000, correlationID++);
        try {
            PerformInteraction pi = new PerformInteraction();
            pi.setInitialPrompt(TTSChunkFactory.createSimpleTTSChunks("init prompt"));
            pi.setInitialText("Init Text");
            pi.setInteractionChoiceSetIDList(Arrays.asList(new Integer[]{CHS_ID_PLAYLISTS}));
            pi.setInteractionLayout(LayoutMode.ICON_ONLY);
            pi.setInteractionMode(InteractionMode.BOTH);
            pi.setTimeout(100000);
            pi.setCorrelationID(correlationID++);
            mSdlProxy.sendRPCRequest(pi);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }




    private void updateGraphicOnSdl(Image graphic) {
        try {
            mSdlProxy.show(null, null, null, null, null, null, null, graphic,
                    null, null, null, correlationID++);

        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void resetGraphiconSdl() {
        Image image = new Image();
        image.setValue("");
        image.setImageType(ImageType.DYNAMIC);
        updateGraphicOnSdl(image);
    }


    public static AppLinkService getInstance() {
        return instance;
    }

    public SdlProxyALM getProxy() {
        return mSdlProxy;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        Log.i(TAG, "onStartCommand");
        startProxy();
        return Service.START_STICKY_COMPATIBILITY;
    }

    public static String _FUNC_() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getMethodName() + "\n";
    }

    public void resetProxy() {
        if (mSdlProxy == null) {
            startProxy();
        } else {
            try {
                mSdlProxy.resetProxy();
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     * called by onStartCommand() to create proxy, if proxy has been created
     * then do nothing, else create a new one.
     */
    public void startProxy() {

//        new Thread() {
//            @Override
//            public void run() {
//                Log.i(TAG, "run: I'm alive");
//                try {
//                    sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
        // check whether current proxy is available, we should avoid to override
        // current proxy. because that will cause Sdl_UNAVAILABLE exception
        if (mSdlProxy != null && mSdlProxy.getIsConnected()) {
            return;
        } else if (mSdlProxy != null) {
            try {
                mSdlProxy.dispose();
            } catch (SdlException e) {

            }
        }
        try {
            Log.i(TAG, "onStartCommand to connect with Sdl using SdlProxyALM");

            // get current system's language to decide HMI language.
            // this is just for this particular app.
            Language language = null;
            Locale locale = Locale.getDefault();
            String lang = locale.getDisplayLanguage();
            Log.i(TAG, "language is " + lang);
//            if (lang.contains("中文")) {
//                language = Language.ZH_CN;
//            } else {
            language = Language.EN_US;
//            }
//            TCPTransportConfig config = new TCPTransportConfig(12345,"192.168.0.1",false);
//            USBTransportConfig config = new USBTransportConfig(this);
//            mSdlProxy = new SdlProxyALM(this,"name",true,Language.EN_US,Language.EN_US,"112121",config);
            mSdlProxy = new SdlProxyALM(this,
                    "AppLink Demo", true, language,
                    language, "1234567890987");
//            mSdlProxy = new SdlProxyALM()
//	      mSdlProxy = new SdlProxyALM()

        } catch (SdlException e) {
            // TODO Auto-generated catch block
            Log.i(TAG, e.getMessage());
//commented to debug
            if (mSdlProxy == null)
                stopSelf();
            e.printStackTrace();
        }

    }

    public void onDestroy() {
        instance = null;

        // dispose proxy when service is destroyed.
        try {
            if (mSdlProxy != null)
                mSdlProxy.dispose();
            mSdlProxy = null;
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "dispose mSdlProxy failed");
            e.printStackTrace();
        }
        Log.i(TAG, "AppLink Service OnDestroy");
        super.onDestroy();
        activity = null;
    }



    public void startMainActivity() {
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("LOCKSCREEN", true);
        startActivity(intent);
    }

    private void sendFileToSdl(int resID, int correalationID, String SdlFileName) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resID);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.PNG, 100, os);
        sentFileToSdl(os.toByteArray(), correalationID, FileType.GRAPHIC_PNG,
                SdlFileName);
    }

    private void sendFileToSdl(AssetManager am, String path, int correlationID,
                               String SdlFileName) {
        try {
            Bitmap bm = BitmapFactory.decodeStream(am.open(path));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bm.compress(CompressFormat.JPEG, 100, os);
            Log.i(TAG, "sendFileToSdl AM: length is " + os.toByteArray().length);
            sentFileToSdl(os.toByteArray(), correlationID,
                    FileType.GRAPHIC_JPEG, SdlFileName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sentFileToSdl(byte[] data, int correlationID, FileType type,
                               String SdlFileName) {
        PutFile putfile = new PutFile();
        putfile.setCorrelationID(correlationID);
        putfile.setBulkData(data);
        putfile.setFileType(type);
        putfile.setSdlFileName(SdlFileName);
        try {
            mSdlProxy.sendRPCRequest(putfile);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendFileToSdl(String filepath, int correalationID,
                               String SdlFileName) {
        Bitmap bitmap = BitmapFactory.decodeFile(filepath);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 100, os);
        sentFileToSdl(os.toByteArray(), correalationID, FileType.GRAPHIC_JPEG,
                SdlFileName);
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        // TODO Auto-generated method stub

        hmilevel = notification.getHmiLevel();

        AudioStreamingState state = notification.getAudioStreamingState();
        Log.i(TAG, "get ononHmiStatus" + state);
        if (firstHMINone) {
//            if(activity != null){
//                activity.setUpMap();
//            }

            activity = MainActivity.getInstance();

            if( listener == null && activity !=null){
                activity.setUpMap();
            }

            Log.i(TAG, "send icon to sdl");
            try {

                mSdlProxy.listfiles(correlationID++);

                boolean isNavAvailable = false;
                HMICapabilities hmicapabilites = mSdlProxy.getHmiCapabilities();
                if (hmicapabilites != null) {
                    isNavAvailable = hmicapabilites.isNavigationAvailable();
                }
                Log.i(TAG, "onOnHMIStatus: isNavigationAvailable " + isNavAvailable);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            sendFileToSdl(R.drawable.ic_launcher, putFileIconID, iconFileName);
            firstHMINone = false;
            try {
                isGen3 = mSdlProxy.getDisplayCapabilities().getGraphicSupported();
                Log.i(TAG, "isGen3 " + isGen3);
            } catch (SdlException e) {
                e.printStackTrace();
            }

        }
        switch (state) {
            case AUDIBLE:

                break;
            case NOT_AUDIBLE:

                break;
            case ATTENUATED:
                break;
        }

        switch (hmilevel) {
            case HMI_BACKGROUND:
                Log.i(TAG, "HMI_BACKGOUND");
                break;
            case HMI_FULL:
                Log.i(TAG, "HMI_FULL");
                // when HMI_FULL arrives, see if the lockscreen is showed.
                // when the app is exited and the user enter the app again, first
                // run is false,so we should make sure the lockscreen is on.

//                showLockscreen();
                if (notification.getFirstRun()) {
                    mWorkThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while(working){
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                getVehicleData();
                            }
                        }
                    });

                    if( !working) working = true;
                    mWorkThread.start();

                } else {
                }
                break;
            case HMI_NONE:
                Log.i(TAG, "HMI_NONE");
                // remove the lockscreen and stop playing music.
            case HMI_LIMITED:
                Log.i(TAG, "HMI_LIMITED");
            default:
                break;
        }

    }


    @Override
    /*
     * (non-Javadoc)
	 *
	 * @see
	 * com.smartdevicelink.proxy.interfaces.IProxyListenerBase#onProxyClosed
	 * (java .lang.String, java.lang.Exception) Called when proxy detects that
	 * connection between Sdl and the Phone breaks
	 */
    public void onProxyClosed(String info, Exception e,
                              SdlDisconnectedReason arg2) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onProxyClosed:" + e.getLocalizedMessage());
        firstHMINone = true;
        working = false;
	  SdlExceptionCause cause = ((SdlException) e).getSdlExceptionCause();
        Log.i(TAG, "onProxyClosed  casue: " + cause.toString());
	  if (cause != SdlExceptionCause.SDL_PROXY_CYCLED
		  && cause != SdlExceptionCause.BLUETOOTH_DISABLED) {
	      if (mSdlProxy != null) {
		  try {
		      mSdlProxy.resetProxy();
		  } catch (SdlException e1) {
		      // TODO Auto-generated catch block
		      e1.printStackTrace();
		  }
	      }
	  } else {
	      // stopSelf();
	  }
    }

    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        // TODO Auto-generated method stub
        Log.i(TAG, "Add command done for " + response.getCorrelationID()
                + " result is " + response.getResultCode());
    }

    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreateInteractionChoiceSetResponse(
            CreateInteractionChoiceSetResponse response) {
        // TODO Auto-generated method stub
        Log.e(TAG, "CreateChoiceSet response: " + response.getSuccess()
                + " code is " + response.getResultCode() + " info is:"
                + response.getInfo() + " correlationID  " + response.getCorrelationID());
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onAlertResponse: " + response.getInfo() + " code:" + response.getResultCode());
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(
            DeleteInteractionChoiceSetResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnCommand(OnCommand notification) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onOnCommand: id：" + notification.getCmdID());
        // get the command id
        int id = notification.getCmdID();
        // get the trigger source, a command can be triggered by pressing menu
        // item in More or using voice command
        TriggerSource ts = notification.getTriggerSource();



        // useless code for setting play mode.
        // if (currentList != null)
        // currentList.setRandom(isRandom);
    }

    @Override
    /*
     * (non-Javadoc)
	 *
	 * @see com.smartdevicelink.proxy.interfaces.IProxyListenerBase#
	 * onPerformInteractionResponse
	 * (com.smartdevicelink.proxy.rpc.PerformInteractionResponse) Handle
	 * response of PerformInteraction
	 */
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        // TODO Auto-generated method stub

        Log.i(TAG, "response " + response.getFunctionName() + " info is " + response.getInfo() + " code is "
                + response.getResultCode() + " Text Entry: " + response.getManualTextEntry());
        // if success continue,else do nothing
        if (response.getSuccess()) {
            // get choice id user has selected. If choices never change, they
            // can be sent to Sdl using RPC CreateInteractionChoiceSet when get
            // HMI_FULL first time. Else, they should be sent before
            // PerformInteraction is called.
            int choiceid = response.getChoiceID();

        }

        // // useless code
        // if (currentList != null)
        // currentList.setRandom(isRandom);
    }

    @Override
    public void onResetGlobalPropertiesResponse(
            ResetGlobalPropertiesResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetGlobalPropertiesResponse(
            SetGlobalPropertiesResponse response) {
        // TODO Auto-generated method stub
        Log.e(TAG, "SetGlobalProperties response: " + response.getSuccess()
                + " code is " + response.getResultCode() + " info is:"
                + response.getInfo());
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        // TODO Auto-generated method stub
        Log.e(TAG, "MediaTimerClock response: " + response.getSuccess()
                + " code is " + response.getResultCode() + " info is:"
                + response.getInfo());
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        // TODO Auto-generated method stub
        Log.e(TAG, "Show response: " + response.getSuccess() + " code is "
                + response.getResultCode() + " info is:" + response.getInfo());
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        // TODO Auto-generated method stub

        ButtonName name = notification.getButtonName();
        Log.i(TAG, "onButtonPress " + name);
        // if button name is custom_button, that means the soft button has been
        // pressed
        // so get the id of the button and do something.

    }

    public void getVehicleData() {
        GetVehicleData vehicledata = new GetVehicleData();
        vehicledata.setSpeed(true);
        vehicledata.setGps(true);
        vehicledata.setCorrelationID(correlationID++);
        try {
            Log.i(TAG, "vehicledata is "
                    + vehicledata.serializeJSON().toString());
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            mSdlProxy.sendRPCRequest(vehicledata);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        // TODO Auto-generated method stub
        try {
            Log.i(TAG, "onOnPermissionsChange: "+notification.serializeJSON().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(PermissionItem permissionItem : notification.getPermissionItem()){
            if( permissionItem.getRpcName().equals("GetVehicleData")) {
                Log.i(TAG, "onOnPermissionsChange Item: "+permissionItem.getRpcName());
                List<String> list = permissionItem.getParameterPermissions().getAllowed();
                if (list != null && list.size() > 0) {
                    for (String allowed : list) {

                        Log.i(TAG, "onOnPermissionsChange allowed: " + allowed);
                    }
                }
            }
        }

    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSubscribeVehicleDataResponse(
            SubscribeVehicleDataResponse response) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(
            UnsubscribeVehicleDataResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        // TODO Auto-generated method stub
        Log.i(TAG, "reponse is " + response.getInfo());
        try {
            Log.i(TAG, "reponse string is "
                    + response.serializeJSON().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (response.getSuccess()) {
            Log.i(TAG,
                    "onGetVehileDataResponse Odometer is : "
                            + response.getOdometer());
            Log.i(TAG,
                    "onGetVehileDataREsponse Speed is : " + response.getSpeed());

            GPSData data = response.getGps();
            Location location = new Location("Sync");
            location.setLatitude(data.getLatitudeDegrees());
            location.setAltitude(data.getAltitude());
            location.setLongitude(data.getLongitudeDegrees());
            location.setSpeed(Float.valueOf(data.getSpeed().toString()));

            onLocationChange(location);
            if(activity != null) {
                activity.updateSpeed(location.getSpeed());
                activity.updateHeading(data.getHeading(), data.getCompassDirection());
            }

        } else if (response.getResultCode().name().equals("DISALLOWED")) {
            Log.i(TAG, "onGetVehicleDataReponse get DISABLLOWED");
//            try {
//                mSdlProxy
//                        .alert("GetVehicleData is Disallowed, please go to Application Settings and request Update",
//                                true, correlationID++);
//            } catch (SdlException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
        }

//        double speed = response.getSpeed().doubleValue();
//        GPSData gpsData = response.getGps();
//        int odometer = response.getOdometer() != null ? response.getOdometer()
//                .intValue() : 0;
//        Log.i(TAG, "speed is " + speed);
//        Log.i(TAG, " odometer is " + odometer);
    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        // TODO Auto-generated method stub

    }

    public void addLocationListener(LocationSource.OnLocationChangedListener listner){

        listener = listner;
    }
    public void onLocationChange(Location location){
        if(listener != null){
            listener.onLocationChanged(location);
        }
    }
    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onOnVehicleData: speed:" + notification.getSpeed());
        Log.i(TAG, "onOnVehicleData: gps:" + notification.getGps());
//        notification.get
    }

    @Override
    public void onPerformAudioPassThruResponse(
            PerformAudioPassThruResponse response) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onPerformAudioPassThru response: " + response.getInfo() + " correlationID: " + response.getCorrelationID());

    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        // TODO Auto-generated method stub
        notification.getAPTData();

    }

    @Override
    public void onPutFileResponse(PutFileResponse response) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onPutFileResponse: " + response.getCorrelationID() + "  "
                + response.getSuccess() + " " + response.getInfo());
        int corID = response.getCorrelationID();
        if (corID == putFileIconID && response.getSuccess()) {
            try {
                mSdlProxy.setappicon("icon.png", correlationID++);
            } catch (SdlException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (corID == putCoverID && response.getSuccess() && mPicToShow) {
            Log.i(TAG, "onPutFile  putCoverID : " + mCoverName);
            Image image = new Image();
            image.setImageType(ImageType.DYNAMIC);
            image.setValue(mCoverName);
            updateGraphicOnSdl(image);
            mPicToShow = false;
        }
    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        // TODO Auto-generated method stub
        // for (String name : response.getFilenames()) {
        // Log.i(TAG, "listFile result: " + name);
        // }

    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSliderResponse(SliderResponse response) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(String arg0, Exception arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnTBTClientState(OnTBTClientState arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnHashChange(OnHashChange arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnSystemRequest(OnSystemRequest arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOnTouchEvent(OnTouchEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDialNumberResponse(DialNumberResponse arg0) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onDialNumberResponse: " + arg0.getInfo() + "  " + arg0.getSuccess());
    }

    @Override
    public void onOnStreamRPC(OnStreamRPC arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSendLocationResponse(SendLocationResponse arg0) {
        // TODO Auto-generated method stub
        Log.i(TAG, "send location response: info " + arg0.getInfo() + " resultCode: " + arg0.getResultCode());
        try {
            mSdlProxy.speak("please confirm", correlationID++);
        } catch (SdlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDataACK() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceEnded(OnServiceEnded arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceNACKed(OnServiceNACKed arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onShowConstantTbtResponse(ShowConstantTbtResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStreamRPCResponse(StreamRPCResponse arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse arg0) {
        // TODO Auto-generated method stub

    }

}
