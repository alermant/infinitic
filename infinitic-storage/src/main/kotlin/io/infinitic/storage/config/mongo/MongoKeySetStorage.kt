/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.storage.config.mongo

import com.mongodb.BasicDBObject
import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import io.infinitic.storage.config.Mongo
import io.infinitic.storage.keySet.KeySetStorage
import org.bson.Document
import org.bson.types.Binary
import org.jetbrains.annotations.TestOnly


private const val MONGO_COLLECTION = "keySetStorage"
private val projectionValueField = Projections.fields(Projections.include("value"), Projections.excludeId())

class MongoKeySetStorage(internal val client: MongoClient, internal val database: String) : KeySetStorage {

  companion object {
    fun from(config: Mongo) = MongoKeySetStorage(config.getPool(), config.database)
  }

  init {
    client.getDatabase(database).let {
      it.createCollection(MONGO_COLLECTION)
      it.getCollection(MONGO_COLLECTION)
          .createIndex(Indexes.text("key"))
    }
  }

  override suspend fun get(key: String): Set<ByteArray> =
      client.getDatabase(database)
          .getCollection(MONGO_COLLECTION)
          .find(Filters.eq("key", key))
          .projection(projectionValueField)
          .mapTo(HashSet()) { (it.getValue("value") as Binary).data }

  override suspend fun add(key: String, value: ByteArray) {
    client.getDatabase(database).getCollection(MONGO_COLLECTION)
        .insertOne(Document().append("key", key).append("value", value))
  }

  override suspend fun remove(key: String, value: ByteArray) {
    client.getDatabase(database).getCollection(MONGO_COLLECTION)
        .deleteMany(
            Filters.and(
                Filters.eq("key", key),
                Filters.eq("value", value)
            )
        )
  }

  override fun close() {
    client.close()
  }

  @TestOnly
  override fun flush() {
    client.getDatabase(database).getCollection(MONGO_COLLECTION)
        .deleteMany(BasicDBObject())
  }
}
