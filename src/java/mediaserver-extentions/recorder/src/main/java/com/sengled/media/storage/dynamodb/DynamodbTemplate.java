package com.sengled.media.storage.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
public class DynamodbTemplate implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamodbTemplate.class);
	
	private DynamoDB dynamoDB = null;
		
	public DynamodbTemplate(DynamoDB dynamoDB){
		this.dynamoDB = dynamoDB;
	}
	
	public void afterPropertiesSet() {
		if (dynamoDB == null) {
			throw new IllegalArgumentException("Property 'dynamoDB' is required");
		}
	}
	
	public boolean isExists(String tableName){
		TableCollection<ListTablesResult> tables = dynamoDB.listTables();
		IteratorSupport<Table, ListTablesResult> it = tables.iterator();
		boolean bool = false;
		while (it.hasNext()) {
			String name = it.next().getTableName();
			if (name.equals(tableName)) {
				bool = true;
				break;
			}
		}
		return bool;
	}
	public void putItem(String tableName,Item item){
		Table table = dynamoDB.getTable(tableName);
		table.putItem(item);
	}
	public void deleteTable(String tableName){
	    Table table = dynamoDB.getTable(tableName);
	    if( null != table ){
	        table.delete();
	    }
	}
	public  DynamoDB getDynamoDB(){
		return dynamoDB;
	}
}
