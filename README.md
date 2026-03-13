This Kotlin code provides a flexible JSON deserialization utility that maps **flat** JSON objects directly to Kotlin data classes (without adapters) using fuzzy key matching.
It is particularly useful when the JSON keys do not exactly match the expected field names (e.g., different naming conventions, typos, or inconsistent APIs).
The core functionality is exposed via an extension function String.parseTo<T>(), which parses a JSON string into an instance of the specified type T.

**Use Case Example**

Suppose you have a JSON response with keys like "user_name", "emailAddr", and you want to map it to a Kotlin data class:

```kotlin
data class User(val username: String, val email: String)
val json = """{"user_name": "john", "emailAddr": "john@example.com"}"""
val user = json.parseTo<User>()
```

The greedyMatch function will match "username" (field) to "user_name" (JSON key) and "email" to "emailAddr" based on bigram similarity, and then coerce the string values appropriately.
