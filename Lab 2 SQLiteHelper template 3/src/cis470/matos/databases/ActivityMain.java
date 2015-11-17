// demonstrates the reading of XML resource files using 
// a SAX XmlPullParser
// ---------------------------------------------------------------------
package cis470.matos.databases;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import org.xmlpull.v1.XmlPullParser;
import java.util.ArrayList;
import java.util.List;

public class ActivityMain extends Activity {

	private TextView txtMsg;
	Button btnGoParser;
	private Button btnWriteSDFile;
	private Button dbBtn;
	private String mySdPath;
	MySQLiteHelper dbs;
	SQLiteDatabase db;
	public String SalaryPerson;
	//private String myDbPath1 = "data/data/cis470.matos.databases/";


	final int MY_PREFS_PRIV_MODE = Activity.MODE_PRIVATE;
	final String MY_PREFS_FILE = "highestsalary";
	// create a reference to the shared preferences object
	SharedPreferences mySharedPreferences;
	// obtain an editor to add data to my SharedPreferences object
	SharedPreferences.Editor myEditor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		txtMsg = (TextView) findViewById(R.id.txtMsg);
		mySdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

		// create a reference to the SharedPreferences file
		mySharedPreferences = getSharedPreferences(MY_PREFS_FILE, MY_PREFS_PRIV_MODE);
		// obtain an editor to add data to (my)SharedPreferences object
		myEditor = mySharedPreferences.edit();
		// has a Preferences file been already created?
//		if (mySharedPreferences != null ) {
		//	applySavedPreferences();
//		} else {
//			Toast.makeText(getApplicationContext(),
//					"No Preferences found", Toast.LENGTH_SHORT).show();
//		}


		btnGoParser = (Button) findViewById(R.id.btnReadXml);
		btnGoParser.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				btnGoParser.setEnabled(false);
				// do slow XML reading in a separated thread
				//Integer xmlResFile = R.xml.manakiki_golf_course;/*************************/
				Integer xmlResFile = R.xml.employees;
				new backgroundAsyncTask().execute(xmlResFile);
			}
		});



		btnWriteSDFile = (Button) findViewById(R.id.button2);
		btnWriteSDFile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					new backgroundAsyncTask2().execute(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}// onClick
		}); // btnWriteSDFile

		dbBtn = (Button) findViewById(R.id.buttondb);
		dbBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent dbIntent = new Intent (ActivityMain.this,
						Activity2.class);
				// create a Bundle (MAP) container to ship data
				// call Activity1, tell your local listener to wait a
				// response sent to a listener known as 101
				startActivityForResult(dbIntent, 101);
			}
		});
		//dbs = new MySQLiteHelper(this);
	}// onCreate

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		try {
			if ((requestCode == 101) && (resultCode == Activity.RESULT_OK)) {
				//Bundle myResultBundle = data.getExtras();
				//Double myResult = myResultBundle.getDouble("result");
				txtMsg.setText("returned from database handler ");
			}
		}
			catch(Exception e){
				txtMsg.setText("Problems - " + requestCode + " " + resultCode);
			}
	}
	/*******************************************************************************************
	*display
    *******************************************************************************************/
	public class backgroundAsyncTask2 extends
			AsyncTask<Integer, Void, List> {

		ProgressDialog dialog = new ProgressDialog(ActivityMain.this);

		List empList = new ArrayList<>();

		String content = null;
		public int highest = 0;

		@Override
		protected void onPostExecute(List empList1) { // after reading xml
			super.onPostExecute(empList1);//
			dialog.dismiss();
			showTable("tableDB");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Please wait...");
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected List doInBackground(Integer... params) {
			return empList;
		}// doInBackground

		private void showTable(String tableName) {
			try {
				String sql = "select * from " + tableName;
				Cursor c = db.rawQuery(sql, null);
				txtMsg.append("\n-showTable: " + tableName + showCursor(c));
			} catch (Exception e) {
				txtMsg.append("\nError showTable: " + e.getMessage());

			}
		}// useCursor1

		private String showCursor(Cursor cursor) {
			// show SCHEMA (column names & types)
			cursor.moveToPosition(-1); //reset cursor's top
			String cursorData = "\nCursor: [";

			try {
				// get column names
				String[] colName = cursor.getColumnNames();
				for (int i = 0; i < colName.length; i++) {
					String dataType = getColumnType(cursor, i);
					cursorData += colName[i] + dataType;

					if (i < colName.length - 1) {
						cursorData += ", ";
					}
				}
			} catch (Exception e) {
				Log.e("<<SCHEMA>>", e.getMessage());
			}
			cursorData += "]";

			// now get the rows
			cursor.moveToPosition(-1); //reset cursor's top
			while (cursor.moveToNext()) {
				String cursorRow = "\n[";
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					cursorRow += cursor.getString(i);
					if (i < cursor.getColumnCount() - 1)
						cursorRow += ", ";
				}
				cursorData += cursorRow + "]";
			}
			return cursorData + "\n";
		}

		private String getColumnType(Cursor cursor, int i) {
			try {
				//peek at a row holding valid data
				cursor.moveToFirst();
				int result = cursor.getType(i);
				String[] types = {":NULL", ":INT", ":FLOAT", ":STR", ":BLOB", ":UNK"};
				//backtrack - reset cursor's top
				cursor.moveToPosition(-1);
				return types[result];
			} catch (Exception e) {
				return " ";
			}
		}
	}

	/*******************************************************************************************
	 *reading XML
	 *******************************************************************************************/
	public class backgroundAsyncTask extends
			AsyncTask<Integer, Void, List> {
		
		ProgressDialog dialog = new ProgressDialog(ActivityMain.this);

		public List<Employee> getEmpList() {
			return empList;
		}

		List<Employee> empList = new ArrayList<>();

		String content = null;
		public int highest = 0;

		@Override
		protected void onPostExecute(List empList1) { // after reading xml
			super.onPostExecute(empList1);//
			dialog.dismiss();

			for(Employee emp: empList) {
				int salary = Integer.parseInt(emp.getSalary());
				if (salary > highest) {
					highest = salary;
					SalaryPerson = emp.getFirstName() + " " + emp.getLastName();
				}
				else
				{}
			}
			//txtMsg.append("highest = " + SalaryPerson);

			myEditor.putInt(SalaryPerson , highest);	// black backgnd
			//myEditor.putString(SalaryPerson, SalaryPerson);	// black backgnd

			myEditor.commit();
			//applySavedPreferences();

			openDatabase(); // open (create if needed) database
			dropTable(); // if needed drop table tblAmigos
			insertSomeDbData();

			Toast.makeText(getApplicationContext(), "completed reading xml",
		Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Please wait...");
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected List doInBackground(Integer... params) {
			Employee emp = null;
			int xmlResFile = params[0];
			XmlPullParser parser = getResources().getXml(xmlResFile);
			String highestSalary;
			int salary;
					
			StringBuilder stringBuilder = new StringBuilder();
			String nodeText = "";
			String nodeName = "";
			try {
				int eventType = -1;
				while (eventType != XmlPullParser.END_DOCUMENT) {

					eventType = parser.next();

					if (eventType == XmlPullParser.START_DOCUMENT) {
						stringBuilder.append("\nSTART_DOCUMENT");

					} else if (eventType == XmlPullParser.END_DOCUMENT) {
						stringBuilder.append("\nEND_DOCUMENT");

					} else if (eventType == XmlPullParser.START_TAG) {
						nodeName = parser.getName();
						stringBuilder.append("\nSTART_TAG: " + nodeName);
						//stringBuilder.append(getAttributes(parser));

						switch(nodeName){
							//Create a new Employee object when the start tag is found
							case "EMPLOYEE":
								emp = new Employee();
								emp.attributes = getAttributes(parser);
								break;
						}
					}
					else if (eventType == XmlPullParser.END_TAG) {
						nodeName = parser.getName();
						switch(nodeName) {
							//For all other end tags the employee has to be updated.
							case "EMPLOYEE":
								empList.add(emp);
								break;
							case "FNAME":
								emp.firstName = nodeText;
								break;
							case "MINIT":
								emp.minit = nodeText;
								break;
							case "LNAME":
								emp.lastName = nodeText;
								break;
							case "SSN":
								emp.ssn = nodeText;
								break;
							case "BDATE":
								emp.bdate = nodeText;
								break;
							case "ADDRESS":
								emp.location = nodeText;
								break;
							case "SEX":
								emp.sex = nodeText;
								break;
							case "SALARY":
								emp.salary = nodeText;
								break;
							case "SUPERSSN":
								emp.superssn = nodeText;
								break;
							case "DNO":
								emp.dno = nodeText;
								break;
						}
						stringBuilder.append("\nEND_TAG:   " + nodeName );

					} else if (eventType == XmlPullParser.TEXT) {
						nodeText = parser.getText();
						stringBuilder.append("\n    TEXT: " + nodeText);
					}
				}
			} catch (Exception e) {
				Log.e("<<PARSING ERROR>>", e.getMessage());
			}

			return empList;
		}// doInBackground

		private String[] getAttributes(XmlPullParser parser) {
			StringBuilder stringBuilder = new StringBuilder();
			// trying to detect inner attributes nested inside a node tag
			String name = parser.getName();
			String[] attributesList = null;
			if (name != null) {
				int size = parser.getAttributeCount();
				attributesList = new String[size];
				for (int i = 0; i < size; i++) {
					String attrName = parser.getAttributeName(i);

					String attrValue = parser.getAttributeValue(i);

					//myMap.put(attrName, attrValue);
					//attributesList[i] = ("key =" + attrName + " value = " + attrValue + "\n");
					attributesList[i] = (attrValue);
					//stringBuilder.append("key =" + attrName + " value = " + attrValue + "\n");
				}
			}
			return attributesList;
		}// innerElements

		private void dropTable() {
			// (clean start) action query to drop table
			try {
				db.execSQL("DROP TABLE IF EXISTS tableDB;");
				txtMsg.append("\n-dropTable - dropped!!");
			} catch (Exception e) {
				txtMsg.append("\nError dropTable: " + e.getMessage());
				finish();
			}
		}

		private void openDatabase() {
			try {
				String myDbPath = mySdPath  + "/myDB1.db";
				txtMsg.append("\n-openDatabase - DB Path: " + myDbPath);

				db = SQLiteDatabase.openDatabase(myDbPath, null,
						SQLiteDatabase.CREATE_IF_NECESSARY);

				txtMsg.append("\n-openDatabase - DB was opened");
			} catch (SQLiteException e) {
				txtMsg.append("\nError openDatabase: " + e.getMessage());
				finish();
			}
		}// createDatabase

		private void insertSomeDbData() {
			// create table: tblAmigo
			db.beginTransaction();
			try {
				// create table
//				db.execSQL("create table tableDB ("
//						+ " ID integer PRIMARY KEY autoincrement, "
//						+ " firstName  text, " + " lastName text , " + " location text );  ");
				// commit your changes
				db.execSQL("create table tableDB ("
						+ " ID integer PRIMARY KEY autoincrement, " +
								" firstName  text, " +
								" minit text , " +
								" lastname text, " +
								" ssn  text, " +
								" bdate text , " +
								" address text, " +
								" sex text , " +
								" salary text);  ");
				// commit your changes


				db.setTransactionSuccessful();

				txtMsg.append("\n-insertSomeDbData - Table was created");

			} catch (SQLException e1) {
				txtMsg.append("\nError insertSomeDbData: " + e1.getMessage());
				finish();
			} finally {
				db.endTransaction();
			}
			// populate table: tblAmigo
			db.beginTransaction();
			try {
				for(Employee emp: empList) {
//					db.execSQL("insert into tableDB(firstName, lastName, location) "
//							+ " values ('"+ (emp.getFirstName())+
//							"', '" + (emp.getFirstName()) + "', '"
//							+ (emp.getLocation())+"', );");
					ContentValues values = new ContentValues();
					values.put("firstName", emp.getFirstName()); // get title
					values.put("minit", emp.getMinit()); // get author
					values.put("lastname", emp.getLastName()); // get author

					values.put("ssn", emp.getSsn()); // get title
					values.put("bdate", emp.getBdate()); // get author
					values.put("address", emp.getLocation()); // get author
					values.put("sex", emp.getSex()); // get title
					values.put("salary", emp.getSalary()); // get author

					// 3. insert
					db.insert("tableDB", // table
							null, //nullColumnHack
							values); // key/value -> keys = column names/ values = column values
				}


				// insert rows
//				db.execSQL("insert into tblAMIGO(name, phone) "
//						+ " values ('BBB', 'aaaaaaaaa' );");
//				db.execSQL("insert into tblAMIGO(name, phone) "
//						+ " values ('BBB', 'bbbbbbbbbb' );");
//				db.execSQL("insert into tblAMIGO(name, phone) "
//						+ " values ('CCC', 'zzzzzzzzzzz' );");



				// commit your changes
				db.setTransactionSuccessful();
				txtMsg.append("\n-insertSomeDbData - 3 rec. were inserted");

			} catch (SQLiteException e2) {
				txtMsg.append("\nError insertSomeDbData: " + e2.getMessage());
			} finally {
				db.endTransaction();
			}
		}// insertSomeData
	}// backroundAsyncTask

	class Employee {
		String id;
		String firstName;
		String minit;
		String lastName;
		String ssn;
		String bdate;

		public String getId() {
			return id;
		}

		public String getMinit() {
			return minit;
		}

		public String getSsn() {
			return ssn;
		}

		public String getBdate() {
			return bdate;
		}

		public String getSex() {
			return sex;
		}

		public String[] getAttributes() {
			return attributes;
		}

		public String getSalary() {
			return salary;
		}

		public String getSuperssn() {
			return superssn;
		}

		String location;
		String sex;
		String [] attributes;
		String salary;
		String superssn;
		String dno;

		@Override
		public String toString() {
			return " FN:" +firstName + "  minit: " + minit +
					" LN: " + lastName + " Address: " + location  + " ssn"
			+ ssn + "  bdate: " + bdate + "  sex: " + sex + "  salary: " + salary +"\n";
		}
		public String getID(){
			return id;
		}
		public String getFirstName(){
			return firstName;
		}
		public String getLastName(){
			return lastName;
		}
		public String getLocation(){
			return location;
		}
	}

	public void applySavedPreferences() {
		// the following setup applies to the (txtMsg) TextView control
		// get <key/value> pairs, use default params for missing data
		int chosenBackColor = mySharedPreferences.getInt("chosenBackColor",Color.BLACK);
		int chosenTextSize = mySharedPreferences.getInt("chosenTextSize", 12);
		String chosenTextStyle = mySharedPreferences.getString("chosenTextStyle", "NORMAL");
		int chosenTextColor = mySharedPreferences.getInt("chosenTextColor", Color.DKGRAY);

		String msg = "color " + chosenBackColor + "\n" + "size " + chosenTextSize
				+ "\n" + "style " + chosenTextStyle;
		//Toast.makeText(getApplicationContext(), msg, 1).show();

		txtMsg.setBackgroundColor(chosenBackColor);
		txtMsg.setTextSize(chosenTextSize);
		txtMsg.setTextColor(chosenTextColor);
		if (chosenTextStyle.compareTo("NORMAL")==0){
			txtMsg.setTypeface(Typeface.SERIF,Typeface.NORMAL);
		}
		else {
			txtMsg.setTypeface(Typeface.SERIF, Typeface.BOLD);
		}

	}// applySavedPreferences
	// ---------------------------------------------------------------------------
	private void usingPreferences(){
		// Save data in a SharedPreferences container
		// We need an Editor object to make preference changes.

		SharedPreferences settings = getSharedPreferences("my_preferred_choices",
				Activity.MODE_PRIVATE );
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("favorite_color", "#ff0000ff");
		editor.putInt("favorite_number", 101);
		editor.commit();

		// retrieving data from SharedPreferences container
		String favColor = settings.getString("favorite_color", "default black");
		int favNumber = settings.getInt("favorite_number", 0);

		//Toast.makeText(this, favColor + " " + favNumber, 1).show();

	}

}// ActivityMain
//Toast.makeText(getApplicationContext(), (String)data.result,
//		Toast.LENGTH_LONG).show();