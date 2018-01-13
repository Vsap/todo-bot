
import slick.jdbc.PostgresProfile.api._



package object repository {
    implicit val localDateTimeToJavaSqlTimestampMapper =
      MappedColumnType.base[java.time.LocalDateTime, java.sql.Timestamp](
        java.sql.Timestamp.valueOf, _.toLocalDateTime)
}
