package com.honeybadger.api;

/*--------------------------------------------------------------------------------------------------------------------------------
 * Version: 1.3
 * Date of last modification: 11SEP13
 * Source Info: n/a
 * 
 * Edit 1.3 (14JUN12): Effected by move of database adapter
 * Edit 4.5 (11SEP13): Revamp of database interaction
 *--------------------------------------------------------------------------------------------------------------------------------
 */

import com.honeybadger.api.databases.DBContentProvider;
import com.honeybadger.api.databases.DBRules;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class Blocker extends Service
{
	private Cursor c;
	private String rule = "";

	private final IBinder mBinder = new MyBinder();

	@Override
	public IBinder onBind(Intent arg0)
	{
		return mBinder;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		c.close();
	}

	public class MyBinder extends Binder
	{
		Blocker getService()
		{
			return Blocker.this;
		}
	}

	/**
	 * Called when service is started; loops through rules in rule database,
	 * generates script of rules to apply to IPTables.
	 */
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		boolean reload = false;
		try
		{
			Bundle extras = intent.getExtras();
			if (extras.getString("reload").contains("true"))
			{
				reload = true;
			}
			else
			{
				reload = false;
			}
		}
		catch (Throwable error)
		{

		}
		
		String target;
		String netInt;

		c = getContentResolver().query(DBContentProvider.CONTENT_URI_RULES, new String[]
				{ DBRules.KEY_ROWID, DBRules.KEY_IP_ADDRESS, DBRules.KEY_PORT,
				DBRules.KEY_DIRECTION, DBRules.KEY_ACTION,
				DBRules.KEY_INTERFACE, DBRules.KEY_DOMAIN,
				DBRules.KEY_SAVED }, null, null, null);
		
		// Loop through rows of database
		while (c.getPosition() < c.getCount() - 1)
		{
			c.moveToNext();
			target = c.getString(1);
			netInt = c.getString(5);
			// If rule has not yet been applied to IPTables, add it to the
			// script string. Generate its components based on the values in the
			// cells.
			if (c.getString(7).contains("false") | reload)
			{
				String drop;
				String inOut;
				if (c.getString(4).contains("allow"))
				{
					drop = "ACCEPT";
				}
				else
				{
					drop = "DROP";
				}

				if (c.getString(3).contains("out"))
				{
					inOut = "OUT";
					if (c.getString(6).contains("domain"))
					{
						rule = SharedMethods.ruleBuilder(this, rule, "Domain", target, true, drop,
								inOut, netInt.contains("wifi"), netInt.contains("cell"));
					}
					else
					{
						rule = SharedMethods.ruleBuilder(this, rule, "IP", target, true, drop,
								inOut, netInt.contains("wifi"), netInt.contains("cell"));
					}
				}
				else
				{
					inOut = "IN";
					if (c.getString(6).contains("domain"))
					{
						rule = SharedMethods.ruleBuilder(this, rule, "Domain", target, true, drop,
								inOut, netInt.contains("wifi"), netInt.contains("cell"));
					}
					else
					{
						rule = SharedMethods.ruleBuilder(this, rule, "IP", target, true, drop,
								inOut, netInt.contains("wifi"), netInt.contains("cell"));
					}
				}

				// Mark rule as having been applied
				getContentResolver().update(Uri.parse(DBContentProvider.CONTENT_URI_RULES + "/" + c.getString(1)), null, null, null);
				SharedMethods.execScript(rule);
			}
			rule = "";
		}
		return START_STICKY;
	}

}
