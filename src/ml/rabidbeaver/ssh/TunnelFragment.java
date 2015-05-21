package ml.rabidbeaver.ssh;

import java.io.File;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TunnelFragment extends Fragment {
	private RecyclerView mRecyclerView;
	private RecyclerView.Adapter<TunnelsAdapter.ViewHolder> mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TunnelManager tunnelManager;
    private Tunnel[] mTunnelArray;
    private LayoutInflater inflater;
    private ViewGroup container;
    
    private Messenger mService = null;
    private boolean mBound = false;
    private Message mMessage = null;
	
	public View onCreateView(final LayoutInflater inf,final ViewGroup con,Bundle savedInstanceState){
		this.inflater=inf;
		this.container=con;
		View v = inflater.inflate(R.layout.tunnels,container,false);
		tunnelManager = new TunnelManager(v.getContext());
		
		ImageButton fab = (ImageButton) v.findViewById(R.id.add_tunnel_button);
		fab.setOutlineProvider(new ViewOutlineProvider() {
	        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
	        @Override
	        public void getOutline(View view, Outline outline) {
	            int diameter = getResources().getDimensionPixelSize(R.dimen.diameter);
	            outline.setOval(0, 0, diameter, diameter);
	        }
	    });
	    fab.setClipToOutline(true);
	    fab.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v) {
				addEditTunnel(null, v);
			}
	    });
		
		mRecyclerView = (RecyclerView) v.findViewById(R.id.tunnels_list);
		mRecyclerView.setHasFixedSize(true);
		
		mLayoutManager = new LinearLayoutManager(v.getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        
        mTunnelArray = tunnelManager.getTunnels();
        mAdapter = new TunnelsAdapter();
        
        mRecyclerView.setAdapter(mAdapter);
		
		return v;
	}
	
	private void addEditTunnel(Tunnel t, final View v){
		// What to do to create a new ssh tunnel.
		// create dialog/form for the tunnel details.
		final View tunnel_form = inflater.inflate(R.layout.tunnel_add_edit, container, false);
		final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
		final String uuid = t!=null?t.getUuid():null;
		final String pri_key_path = t!=null?t.getIdPriPath():null;
		builder.setTitle(t!=null?"Edit Tunnel":"New Tunnel");
		builder.setPositiveButton(t!=null?"Save Changes":"Add Tunnel", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String tunnel_name = ((EditText) tunnel_form.findViewById(R.id.tunnel_name)).getText().toString();
				String tunnel_server = ((EditText) tunnel_form.findViewById(R.id.tunnel_server)).getText().toString();
				int tunnel_server_port = Integer.parseInt(((EditText) tunnel_form.findViewById(R.id.tunnel_server_port)).getText().toString());
				String tunnel_user = ((EditText) tunnel_form.findViewById(R.id.tunnel_user)).getText().toString();
				String tunnel_pubkey = ((EditText) tunnel_form.findViewById(R.id.tunnel_pubkey)).getText().toString();
				String tunnel_prikey = ((EditText) tunnel_form.findViewById(R.id.tunnel_prikey)).getText().toString();
				String tunnel_host = ((EditText) tunnel_form.findViewById(R.id.tunnel_host)).getText().toString();
				int tunnel_host_port = Integer.parseInt(((EditText) tunnel_form.findViewById(R.id.tunnel_host_port)).getText().toString());
				int tunnel_local_port = Integer.parseInt(((EditText) tunnel_form.findViewById(R.id.tunnel_local_port)).getText().toString());
				boolean tunnel_reverse = ((CheckBox) tunnel_form.findViewById(R.id.tunnel_reverse)).isChecked();
				
				if (tunnel_name.length() > 0 && tunnel_server.length() > 0 && tunnel_server_port > 0
						&& tunnel_user.length() > 0 && tunnel_host.length() > 0 && tunnel_host_port > 0
						&& tunnel_local_port > 0 && tunnel_prikey.length() > 0){ // if everything checks out...
					
					tunnelManager.addOrUpdateTunnel(tunnel_name, tunnel_server, tunnel_server_port, tunnel_local_port, tunnel_host,
							tunnel_host_port, tunnel_user, tunnel_reverse, tunnel_pubkey, pri_key_path, tunnel_prikey, uuid);
					
					// refresh the TunnelsFragment.
					TunnelFragment.this.refresh();
					
					// copy public key to clipboard
					ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Public Key", tunnel_pubkey);
					clipboard.setPrimaryClip(clip);
					
					//close dialog
					dialog.dismiss();
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.setView(tunnel_form);
		
		if (t != null){
			((EditText) tunnel_form.findViewById(R.id.tunnel_name)).setText(t.getName());
			((EditText) tunnel_form.findViewById(R.id.tunnel_server)).setText(t.getSSHHost());
			((EditText) tunnel_form.findViewById(R.id.tunnel_server_port)).setText(Integer.toString(t.getSSHPort()));
			((EditText) tunnel_form.findViewById(R.id.tunnel_user)).setText(t.getUserName());
			((EditText) tunnel_form.findViewById(R.id.tunnel_pubkey)).setText(t.getIdPub());
			((EditText) tunnel_form.findViewById(R.id.tunnel_prikey)).setText(t.getIdPri());
			((EditText) tunnel_form.findViewById(R.id.tunnel_host)).setText(t.getHost());
			((EditText) tunnel_form.findViewById(R.id.tunnel_host_port)).setText(Integer.toString(t.getHostPort()));
			((EditText) tunnel_form.findViewById(R.id.tunnel_local_port)).setText(Integer.toString(t.getLocalPort()));
			((CheckBox) tunnel_form.findViewById(R.id.tunnel_reverse)).setChecked(t.isReverse());
		} else ((EditText) tunnel_form.findViewById(R.id.tunnel_server_port)).setText("22");
		
		Button genkeys = (Button) tunnel_form.findViewById(R.id.tunnel_genkeys);
		genkeys.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(final View v) {
				Log.d("TUNNELSFRAGMENT","Pressed GENERATE KEYS button");
				final String tmppath=v.getContext().getApplicationInfo().dataDir+"/files";
				if (RootShell.isAccessGiven()){
					String cmd1 = "/system/bin/ssh-keygen -f "+tmppath+"/tmp-id_rsa -N '' -t rsa";
					String cmd2 = "chmod 666 "+tmppath+"/tmp-id_*";
					String cmd3 = "OWNER=`ls -lad tmppath`";
					String cmd4 = "chown $OWNER.$OWNER "+tmppath+"/*";
					Command command = new Command(0, cmd1, cmd2, cmd3, cmd4){
						@Override
					    public void commandCompleted(int id, int exitcode) {
							String prikey = FileIO.readFromFile(v.getContext(), "tmp-id_rsa");
							Log.d("PRIKEY",prikey);
							String pubkey = FileIO.readFromFile(v.getContext(), "tmp-id_rsa.pub");
							Log.d("PUBKEY",pubkey);
							new File(tmppath+"/tmp-id_rsa").delete();
							new File(tmppath+"/tmp-id_rsa.pub").delete();
							EditText pub_key = (EditText) tunnel_form.findViewById(R.id.tunnel_pubkey);
							EditText pri_key = (EditText) tunnel_form.findViewById(R.id.tunnel_prikey);
							pub_key.setText(pubkey);
							pub_key.setEnabled(false);
							pri_key.setText(prikey);
							pri_key.setEnabled(false);
						}
					};
					try {
						// I don't want to run this as root, but something weird happens and it crashes
						// hard on nexus 9 when run as a normal user. Could be selinux related?
						RootShell.getShell(true).add(command);
						RootShell.closeShell(true);
					} catch (Exception e) { Log.d("EXCEPTION",e.getLocalizedMessage()); }
				}
			}
		});
		
		builder.create().show();
	}
	
	private void refresh(){
		mTunnelArray = tunnelManager.getTunnels();
		mAdapter.notifyDataSetChanged();
	}
	
	private class TunnelsAdapter extends RecyclerView.Adapter<TunnelsAdapter.ViewHolder> {
	    public class ViewHolder extends RecyclerView.ViewHolder {
	        public CardView mCardView;
	        public ViewHolder(CardView v) {
	            super(v);
	            mCardView = v;
	        }
	    }
	    
	    @Override
	    public TunnelsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
	        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tunnel_view, parent, false);
	        ViewHolder vh = new ViewHolder((CardView)v);
	        return vh;
	    }

	    @Override
	    public void onBindViewHolder(ViewHolder holder, final int position) {
	    	((TextView)holder.mCardView.findViewById(R.id.tun_name)).setText(mTunnelArray[position].getName().toUpperCase());
	    	((ImageButton)holder.mCardView.findViewById(R.id.card_expand_button)).setOnClickListener(new ImageButton.OnClickListener(){
				@Override
				public void onClick(View v) {
					CardView c = (CardView) v.getParent().getParent().getParent();
					c.findViewById(R.id.expansion_layout).setVisibility(View.VISIBLE);
					v.setVisibility(View.INVISIBLE);
				}
	    	});
	    	((ImageButton)holder.mCardView.findViewById(R.id.card_collapse_button)).setOnClickListener(new ImageButton.OnClickListener(){
				@Override
				public void onClick(View v) {
					CardView c = (CardView) v.getParent().getParent().getParent().getParent();
					c.findViewById(R.id.card_expand_button).setVisibility(View.VISIBLE);
					c.findViewById(R.id.expansion_layout).setVisibility(View.GONE);
				}
	    	});
	    	
	    	((TextView)holder.mCardView.findViewById(R.id.cv_server)).setText(mTunnelArray[position].getSSHHost());
	    	((TextView)holder.mCardView.findViewById(R.id.cv_port)).setText(Integer.toString(mTunnelArray[position].getSSHPort()));
	    	((TextView)holder.mCardView.findViewById(R.id.cv_user)).setText(mTunnelArray[position].getUserName());
	    	if (mTunnelArray[position].getIdPub().length() > 10){
	    		((LinearLayout)holder.mCardView.findViewById(R.id.cv_row_pubkey)).setVisibility(View.VISIBLE);
	    		((Button)holder.mCardView.findViewById(R.id.copy_pubkey)).setVisibility(View.VISIBLE);
	    	}
	    	((TextView)holder.mCardView.findViewById(R.id.cv_pubkey)).setText(mTunnelArray[position].getIdPub());
	    	((TextView)holder.mCardView.findViewById(R.id.cv_tun_host)).setText(mTunnelArray[position].getHost());
	    	((TextView)holder.mCardView.findViewById(R.id.cv_tun_port)).setText(Integer.toString(mTunnelArray[position].getHostPort()));
	    	((TextView)holder.mCardView.findViewById(R.id.cv_local_port)).setText(Integer.toString(mTunnelArray[position].getLocalPort()));
	    	((TextView)holder.mCardView.findViewById(R.id.cv_direction)).setText(mTunnelArray[position].isReverse()?"Reverse":"Forward");
	    	
	    	((Button)holder.mCardView.findViewById(R.id.start_hold_button)).setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					// TODO change this to a START service, for manual tunnel bringup/down. It is only suitable for
					// externals to use bind service, in order to support auto-teardown.
					
					// In order to track the specific binder connection, the service needs to have enough
					// information on the client to be able to track when the client process has terminated.
					// The client pid + uid (which is attached to the message automatically) will suffice.
					Message msg = Message.obtain(null, TunnelService.MSG_HOLDOPEN_TUNNEL, android.os.Process.myPid(), 0, mTunnelArray[position].getUuid());
					if (!mBound){
						TunnelFragment.this.getActivity().bindService(new Intent(v.getContext(), TunnelService.class), mConnection, Context.BIND_AUTO_CREATE);
						mMessage = msg;
					} else {
						try {
							mService.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
	    	});
	    	((Button)holder.mCardView.findViewById(R.id.edit_button)).setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					Tunnel mTunnel = mTunnelArray[position];
					addEditTunnel(mTunnel, v);
				}
	    	});
	    	((Button)holder.mCardView.findViewById(R.id.copy_pubkey)).setOnClickListener(new Button.OnClickListener(){
				@Override
				public void onClick(View v) {
					LinearLayout l = (LinearLayout) v.getParent().getParent().getParent();
					ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Public Key", ((TextView)l.findViewById(R.id.cv_pubkey)).getText().toString());
					clipboard.setPrimaryClip(clip);
					Toast.makeText(v.getContext(), "Public Key Copied to Clipboard",Toast.LENGTH_SHORT).show();
				}
	    	});
	    }

	    @Override
	    public int getItemCount() {
	        return mTunnelArray.length;
	    }
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	Log.d("TUNNELFRAGMENT","setting up service");
            mService = new Messenger(service);
            mBound = true;
            if (mMessage != null){
            	try {
					mService.send(mMessage);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };
    
    @Override
	public void onStop(){
    	super.onStop();
    	if (mBound){
    		Message msg = Message.obtain(null, TunnelService.MSG_DROP_ALL_TUNNELS, android.os.Process.myPid(), 0);
    		try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    		TunnelFragment.this.getActivity().unbindService(mConnection);
    		mBound=false;
    	}
    }
}