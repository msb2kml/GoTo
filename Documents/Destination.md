# Destination selection

There are several ways to specify the destination to reach.  
The order of priority is the order of description below.

## By Msb2And

At the end of the flight if the last known position of the plane is
far from the start location, **Msb2And** could launch specifically
the **GoTo** application with a special Intent describing this position.
This location contains an altitude and a name is bundled under the
heading "name".  
Technically, the location is specified by a ParcelableExtra named
"Target".

## By a "geo" URI

The application registers itself to receive any URI of the "geo" type.  
Several forms are accepted:

+ geo:50.894667,4.342549
+ geo:0,0?q=50.894667,4.342549
+ geo:0,0?q=50.894667,4.342549(Atomium)

A street address is not accepted as there is no access to Internet.  
No altitude is available.

This type of notation could be found on Web pages, sent by mail or
embedded in a QR code.

## By a "file" URI

The application registers itself to process files with the extension "gpx".  
The file is read and all waypoints are listed: name, latitude, longitude
and altitude.  
Then the user selects the waypoint that is to be the destination.  
The altitude could be available or not.

## By user selection

The application has been launched directly by the user or none of the
previous specifications have been recognized.  
A specialized file explorer present the directories and GPX type files.
It is possible to go up or down in the hierarchy of directories.
There could be two roots for this hierarchy: **"[\*Fixed\*]"** storage
and **"[\*Removable\*]"** storage.  
Once a file has been selected a choice of waypoint is proposed as for
the previous method.

## Missing data (any method)

If the altitude of the destination is not available the relative height
will not be displayed.

If no name is specified, a name of "Target" is assumed.


