package org.sparrow.thrift;

import org.apache.thrift.TException;
import org.sparrow.config.DatabaseDescriptor;
import org.sparrow.db.SparrowDatabase;
import org.sparrow.spql.SpqlParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by mauricio on 24/12/2015.
 */
public class TServerTransportHandler implements SparrowTransport.Iface
{
    @Override
    public String authenticate(String username, String password) throws TException
    {
        return null;
    }

    @Override
    public String logout() throws TException
    {
        return null;
    }

    @Override
    public List<String> show_databases() throws TException
    {
        List<String> list = new ArrayList<>(SparrowDatabase.instance.getDatabases());
        Collections.sort(list, (str1, str2) -> str1.toUpperCase(Locale.getDefault()).compareTo(str2.toUpperCase(Locale.getDefault())));
        return list;
    }

    @Override
    public String create_database(String dbname) throws TException
    {
        boolean result = SparrowDatabase.instance.createDatabase(dbname);
        return result ? "Database " + dbname + " created" : "Could not create database " + dbname;
    }

    @Override
    public String drop_database(String dbname) throws TException
    {
        boolean result = SparrowDatabase.instance.dropDatabase(dbname);
        return result ? "Database " + dbname + " dropped" : "Could not drop database " + dbname;
    }

    @Override
    public String insert_data(DataObject object) throws TException
    {
        if (object.getSize() < DatabaseDescriptor.config.max_datalog_size)
        {
            if (SparrowDatabase.instance.databaseExists(object.getDbname()))
            {
                SparrowDatabase.instance.insert_data(object);
                return "";
            }
        }
        return "Could not insert data";
    }

    @Override
    public String delete_data(String dbname, String key) throws TException
    {
        if (SparrowDatabase.instance.databaseExists(dbname))
        {
            SparrowDatabase.instance.delete_data(dbname, key);
            return "";
        }
        return "Data deleted";
    }

    @Override
    public SpqlResult spql_query(String query) throws TException
    {
        SpqlResult result = SpqlParser.parseAndProcess(query);
        if (result == null)
            return new SpqlResult();
        return result;
    }
}
