This Kotlin code provides a flexible JSON deserialization utility that maps flat JSON objects to Kotlin data classes using fuzzy key matching.
It is particularly useful when the JSON keys do not exactly match the expected field names (e.g., different naming conventions, typos, or inconsistent APIs).
The core functionality is exposed via an extension function String.parseTo<T>(), which parses a JSON string into an instance of the specified type T.
