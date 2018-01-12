package com.seamfix.kyc.bfp.syncs;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import com.seamfix.kyc.bfp.proxy.BasicData;
import com.seamfix.kyc.bfp.proxy.DynamicData;
import com.seamfix.kyc.bfp.proxy.EnrollmentLog;
import com.seamfix.kyc.bfp.proxy.FileSyncNewEntryMarker;
import com.seamfix.kyc.bfp.proxy.FingerReasonCodes;
import com.seamfix.kyc.bfp.proxy.MetaData;
import com.seamfix.kyc.bfp.proxy.MsisdnDetail;
import com.seamfix.kyc.bfp.proxy.Passport;
import com.seamfix.kyc.bfp.proxy.PassportDetail;
import com.seamfix.kyc.bfp.proxy.RegistrationSignature;
import com.seamfix.kyc.bfp.proxy.Signature;
import com.seamfix.kyc.bfp.proxy.SpecialData;
import com.seamfix.kyc.bfp.proxy.State;
import com.seamfix.kyc.bfp.proxy.SubscriberTypes;
import com.seamfix.kyc.bfp.proxy.WsqImage;
import com.sf.biocapture.entity.EnrollmentRef;
import com.sf.biocapture.entity.UserId;
public class BiocaptureObjectInputStream extends ObjectInputStream{
	private Object currentObject;
	private boolean isReading;
	private boolean hasException;
	private Exception ex;
	private Timer timer;
	public BiocaptureObjectInputStream(InputStream in) throws IOException {
		super(in);
	}
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		Class<?> clazz = null;
		if(desc.getName().equals("com.sf.biocapture.main.entity.userid.UserId")){
			clazz = (UserId.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.basicdata.BasicData")){
			clazz = (BasicData.class);
		}else if(desc.getName().equals("com.sf.biocapture.dynamic.entity.dynamicdata.DynamicData")){
			clazz = (DynamicData.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.state.State")){
			clazz = (State.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.enrollmentlog.EnrollmentLog")){
			clazz = (EnrollmentLog.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.enrollmentref.EnrollmentRef")){
			clazz = (EnrollmentRef.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.metadata.MetaData")){
			clazz = (MetaData.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.msisdndetail.MsisdnDetail")){
			clazz = (MsisdnDetail.class);
		}else if(desc.getName().equals("com.sf.biocapture.verified.entity.passport.Passport")){
			clazz = (Passport.class);
		}else if(desc.getName().equals("com.sf.biocapture.verified.entity.wsqimage.WsqImage")){
			clazz = (WsqImage.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.msisdndetail.SubscriberTypes")){
			clazz = (SubscriberTypes.class);
		}else if(desc.getName().equals("com.sf.biocapture.verified.entity.wsqimage.FingerReasonCodes")){
			clazz = (FingerReasonCodes.class);
		}else if(desc.getName().equals("com.sf.biocapture.util.FileSyncNewEntryMarker")){
			clazz = (FileSyncNewEntryMarker.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.passportdetail.PassportDetail")){
			clazz = (PassportDetail.class);
		}else if(desc.getName().equals("com.sf.biocapture.verified.entity.signature.Signature")){
			clazz = (Signature.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.specialdata.SpecialData")){
			clazz = (SpecialData.class);
		}else if(desc.getName().equals("com.sf.biocapture.main.entity.registrationsignature.RegistrationSignature")){
			clazz = (RegistrationSignature.class);
		}else{
			clazz = super.resolveClass(desc);
		}
		return clazz;
	}
	/**
	 *
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	protected void readKycObject() throws IOException, ClassNotFoundException {
		timer = new Timer(false);
		timer.schedule(new StreamReader(), 0);
	}
	protected Object waitForObject() throws ClassNotFoundException, IOException {
		isReading = true;
		hasException = false;
		ex = null;
		currentObject = new DummyObject();
		readKycObject();
		Long start = new Date().getTime();
		while(isReading){
			if(hasException){
				if(ex instanceof ClassNotFoundException){
					throw new ClassNotFoundException("Exception", ex);
				}else{
					throw new IOException(ex);
				}
			}
			if(!(currentObject instanceof DummyObject)){
				isReading = false;
				break;
			}else if(new Date().getTime() - start > 10000){
				timer.cancel();
				throw new BiocaptureStreaException();
			}else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
		}
		return currentObject;
	}
	class StreamReader extends TimerTask{
		@Override
		public void run() {
			try {
				currentObject = readObject();
				timer.cancel();
			} catch (ClassNotFoundException | IOException e) {
				timer.cancel();
				hasException = true;
				ex = e;
				e.printStackTrace();
			}
		}
	}
	class DummyObject {
	}
	class BiocaptureStreaException extends RuntimeException {
		/**
		 *
		 */
		private static final long serialVersionUID = -2751502856104330711L;
		public BiocaptureStreaException() {
			super("bfphobia cuasing sync file found");
		}
	}
}