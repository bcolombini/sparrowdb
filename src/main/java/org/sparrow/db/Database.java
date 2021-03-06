package org.sparrow.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparrow.cache.CacheFactory;
import org.sparrow.cache.ICache;
import org.sparrow.config.DatabaseDescriptor;
import org.sparrow.thrift.DataObject;
import org.sparrow.thrift.SpqlResult;
import org.sparrow.util.FileUtils;
import org.sparrow.util.SPUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by mauricio on 25/12/2015.
 */
public class Database
{
    private static Logger logger = LoggerFactory.getLogger(Database.class);
    private static final String FILENAME_EXTENSION = ".spw";
    private volatile Set<DataHolder> dataHolders;
    private volatile DataLog dataLog;
    private String dbname;
    private ICache<String, DataDefinition> cache = CacheFactory.newCache(80);

    private Database(String dbname)
    {
        this.dbname = dbname;
        dataHolders = new LinkedHashSet<>();
        dataLog = new DataLog(dbname, dataHolders, SPUtils.getDbPath(dbname, "datalog", FILENAME_EXTENSION));
    }

    public static Database build(String dbname)
    {
        Database database = null;
        try
        {
            FileUtils.createDirectory(DatabaseDescriptor.getDataFilePath() + dbname);
            database = new Database(dbname);
        }
        catch (Exception e)
        {
            e.getMessage();
        }
        return database;
    }

    public static Database open(String dbname)
    {
        Database database = new Database(dbname);

        if (!database.dataLog.isEmpty())
        {
            logger.debug("Loading datalog {} with size: {}", dbname, database.dataLog.getSize());
            database.dataLog.load();
        }

        DataHolder.DataHolderFileManager.loadDataHolders(database.dataHolders, dbname);

        return database;
    }


    public void close()
    {
        cache.clear();
        dataHolders.clear();
        dataLog.close();
    }

    public void insertData(DataObject object)
    {
        int hash32key = SPUtils.hash32(object.getKey());
        DataDefinition dataDefinition = new DataDefinition();
        dataDefinition.setKey(object.getKey());
        dataDefinition.setKey32(hash32key);

        /*
         *  As append only data file, the offset of new data is the
         *   the size of data file. It is updated when the data is
         *   written to the file.
        */
        dataDefinition.setOffset(0);

        // Get current time int UTC
        dataDefinition.setTimestamp(java.time.Instant.now().getEpochSecond());
        dataDefinition.setSize(object.bufferForData().capacity());
        dataDefinition.setExtension(DataDefinition.Extension.PNG);
        dataDefinition.setState(DataDefinition.DataState.ACTIVE);
        dataDefinition.setBuffer(object.bufferForData().array());
        insertData(dataDefinition);
    }

    public void insertData(DataDefinition dataDefinition)
    {
        dataLog.add(dataDefinition);
    }

    public DataDefinition getDataWithImageByKey32(String dataKey)
    {
        DataDefinition dataDefinition = cache.get(dataKey);

        if (dataDefinition == null)
        {
            dataDefinition = dataLog.get(dataKey);
        }

        if (dataDefinition == null)
        {
            Iterator<DataHolder> iterDataHolder = dataHolders.stream()
                    .filter(x -> x.isKeyInFile(dataKey))
                    .iterator();

            while (iterDataHolder.hasNext())
            {
                dataDefinition = iterDataHolder.next().get(dataKey);
            }
        }

        if (dataDefinition != null)
        {
            cache.put(dataKey, dataDefinition);
        }

        return dataDefinition;
    }

    public boolean deleteData(String dataKey)
    {
        boolean isDataInCache = cache.containsKey(dataKey);

        DataDefinition dataDefinition = getDataWithImageByKey32(dataKey);

        if (dataDefinition == null)
        {
            return false;
        }
        else
        {
            Tombstone tombstone = new Tombstone(dataDefinition);
            dataLog.add(tombstone);
            cache.put(dataKey, tombstone);
        }

        return true;
    }

    public SpqlResult mapToSpqlResult(Set<DataDefinition> data)
    {
        SpqlResult result = new SpqlResult();
        for(DataDefinition dataDefinition : data)
        {
            DataObject dataObject = new DataObject();
            dataObject.setDbname(dbname);
            dataObject.setSize(dataDefinition.getSize());
            dataObject.setTimestamp(dataDefinition.getTimestamp());
            dataObject.setKey(dataDefinition.getKey());
            dataObject.setState(dataDefinition.getState().ordinal());
            result.addToRows(dataObject);
        }
        result.count = data.size();
        return  result;
    }

    public SpqlResult queryDataWhereKey(String value)
    {
        DataDefinition dataDefinition = getDataWithImageByKey32(value);
        SpqlResult result = new SpqlResult();
        if (dataDefinition!=null)
        {
            result = mapToSpqlResult(new HashSet<DataDefinition>(){{
                add(dataDefinition);
            }});
        }
        return result;
    }

    public SpqlResult queryDataAll()
    {
        Set result = new LinkedHashSet<>();
        dataHolders.forEach(x -> result.addAll(x.fetchAll()));
        return mapToSpqlResult(result);
    }
}