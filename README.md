Open mHealth FitBit Shim
-------------

A shim that runs inside of the [Shim DSU](https://github.com/openmhealth/shim) and converts a subset of Fitbit data streams to Open mHealth conformant data streams.

### Data Schemas

| Source | ID | Version | Concordia Schema |
| ---- | ---- | ---- | ---- |
| Minutes Asleep | `omh:fitbit:time_asleep_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:time_asleep_minutes/1/1) |
| Minutes Lightly Active | `omh:fitbit:lightly_active_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:lightly_active_minutes/1) |
| Floors Climbed | `omh:fitbit:floors` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:floors/1) |
| Elevation in Feet| `omh:fitbit:elevation_ft` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:elevation_ft/1) |
| Minutes in Bed | `omh:fitbit:time_in_bed_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:time_in_bed_minutes/1) |
| Activity Calories | `omh:fitbit:activity_calories` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:activity_calories/1) |
| Sedentary Minutes  | `omh:fitbit:sedentary_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:sedentary_minutes/1) |
| Calories | `omh:fitbit:calories` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:calories/1) |
| Distance in Miles | `omh:fitbit:distance_mi` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:distance_mi/1) |
| Fairly Active Minutes | `omh:fitbit:fairly_active_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:fairly_active_minutes/1) |
| Steps | `omh:fitbit:steps` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:steps/1) |
| Very Active Minutes | `omh:fitbit:very_active_minutes` | `1` | [Schema](https://go.omh.io/omh/v1/omh:fitbit:very_active_minutes/1) |


### Build and Deploy

To build the shim, install [ant](http://ant.apache.org/) and run `ant clean dist`.

When deploying into the Shim DSU, the jar file built in the above step, `fitbit4j-1.0.25.jar` (located in the `lib` directory in this repo) and [json-20090211.jar](http://mvnrepository.com/artifact/org.json/json/20090211) must be placed into the Shim DSU's `WEB-INF/lib` directory.


### Configuration

`fitbit.clientId` and `fitbit.clientSecret` must be set in the DSU system properties file `WEB-INF/config/default.properties`. A key and secret can be obtained [from FitBit](https://dev.fitbit.com/apps/new).


A listener entry must also be added to the DSU's `WEB-INF/web.xml` file.

    <listener>
        <listener-class>
            org.openmhealth.shim.fitbit.FitBitShimRegistry
        </listener-class>
    </listener>

### Run and Test

To run the shim, restart your servlet container and navigate to the following path in a web browser to view the entries in your DSU's registry. 

    /omh/v1/

The IDs referenced in the Data Schemas table above will be present.


### Troubleshooting

The Shim DSU uses [log4j](http://logging.apache.org/log4j/1.2/) for application logging. The application writes logging messages to the default location specified by your serlvet container. Check the logging messages to see what went wrong.

In most cases, problems with shims can be fixed by making sure the deployment and configuration steps were correctly followed.

For any other issues, you can post to our [discussion group](https://groups.google.com/forum/#!forum/omh-developers).




