package clients;

import clients.UserProtos.UserOuterClass.User;

/** Parses one pipe-separated line of users-data-incr.csv into a Protobuf User. */
final class Users {
  private Users() {}

  static User parse(String line) {
    final String[] values = line.split("\\|", -1);
    if (values.length != 7) {
      throw new IllegalArgumentException("Expected 7 fields, got " + values.length);
    }
    return User.newBuilder()
        .setId(Integer.parseInt(values[0]))
        .setFirstName(values[1])
        .setLastName(values[2])
        .setEmail(values[3])
        .setBirthday(values[4])
        .setRegTimestamp(Long.parseLong(values[5]))
        .setActiveAccount(parseBoolean(values[6]))
        .build();
  }

  // Boolean.parseBoolean turns anything that is not "true", even "", into false
  private static boolean parseBoolean(String value) {
    if (!value.equals("true") && !value.equals("false")) {
      throw new IllegalArgumentException("ActiveAccount must be true or false, got \"" + value + "\"");
    }
    return Boolean.parseBoolean(value);
  }
}
