package rocks.twr.core.app_out;

public record TwrOutbox(
        /** used by aggregator to work out what to do */
        Type type,

        /** the topic to which this row needs to ultimately be sent to for processing by the app, e.g. connect-txoutbox-<srcDatabase>-out */
        String targetTopic,

        /** the name of the database from which this row comes */
        String srcDatabase,

        /** the name of the table from which this row comes */
        String table,

        /** ms since epoch to delay processing in the app until */
        long delayUntil,

        /** business key, as provided by developer - used for partitioning */
        String aggregateId,

        /** app specific used to find executor */
        String aggregateType,

        /** used by the framework to calculate the new delayUntil */
        String numAttempts,

        /** if true, then such records don't have to wait for waiting records to be completed */
        String ignoreOrdering,

        /** a timestamp to say when the row was set to be ignored */
        String ignoreTask,



        /** PK out of db - used for determining uniqueness */
        String id,

        /** json string provided by app, and provided as input to the executor */
        String aggregate,

        /** db timestamp, epoch millis, when the record was created in the database, just for info if required, e.g. 1684090539000 */
        long created,

        /** @see created */
        long updated,

        /** nullable; a timestamp to say when the row was successfully completed; @see created */
        Long completed,

        /** nullable; a timestamp to say when the row was unsuccessfully executed and set to failed, blocking everything for the aggregateId; @see created */
        Long failed
) {

}
