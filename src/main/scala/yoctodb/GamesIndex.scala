// Copyright (c) 2021-22 by Vadim Bondarev
// This software is licensed under the Apache License, Version 2.0.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

package yoctodb

import com.yandex.yoctodb.query.Order
import com.yandex.yoctodb.util.UnsignedByteArrays.from
import com.yandex.yoctodb.v1.immutable.V1Database
import yoctodb.schema.games.v1.GamesSchema
import yoctodb.schema.games.v1.GamesSchema.Pcolumn
import yoctodb.schema.games.v1.GamesSchema.*

import CEntry.*

import mazboot.net.*
import mazboot.validations.*

object GamesIndex:

  val IndexName = "games"
  val PayloadColumnName = "g_payload"
  val InfoColumnName = "g_info"

  // Columns
  val fullStage = GameFullStage()
  val awayTeam = GameAwayTeam()
  val homeTeam = GameHomeTeam()
  val year = GameYear()
  val month = GameMonth()
  val day = GameDay()
  val gameTime = GameTime()
  val winner = GameWinner()
  // val fake     = new Fake()

  val IndexColumns: Set[GamesSchema.Pcolumn] =
    Set(
      fullStage.protoColumn,
      awayTeam.protoColumn,
      homeTeam.protoColumn,
      year.protoColumn,
      month.protoColumn,
      day.protoColumn,
      winner.protoColumn,
      gameTime.protoColumn,
    )

  type Filterable =
    Column[GameHomeTeam & GameWinner & GameYear & (GameFullStage & GameAwayTeam & (GameMonth & GameDay))]

  // Precisely defined filterable schema of the GamesIndex as a value of intersection|products type
  val FilterableSegment /*: Filterable*/ =
    Column(fullStage) ++ Column(awayTeam) ++ Column(homeTeam) ++ Column(winner) ++ Column(year) ++ Column(
      month
    ) ++ Column(day) // ++ Fake(fake)

  type Sortable = Column[GameTime & GameYear & (GameMonth & GameDay)]

  // Precisely defined sortable schema of the GamesIndex
  val SortableSegment /*: Sortable*/ =
    Column(gameTime) ++ Column(year) ++ Column(month) ++ Column(day)

  // **************************************Proves**********************************************************************/

  type Union =
    Column[GameTime] | Column[GameYear] | Column[GameMonth] |
      Column[GameDay] // union types(sum types, modeled as sealed traits)

  type Intersection =
    Column[GameTime] & Column[GameDay] & Column[GameTime] // intersection types (products, modeled as case classes)

  summon[Union <:< Column[CEntry[?]]]
  // Union type is a subtype of Column[CEntry[?]] or, differently expressed: an instance of Union can be considered as being an instance of Column[CEntry[?]].

  summon[Intersection <:< Column[CEntry[?]]]
  // Intersection type is a subtype of Column[CEntry[?]] or, differently expressed: an instance of Union can be considered as being an instance of Column[CEntry[?]].

  implicitly[Column[GameTime] with Column[GameDay] <:< Column[CEntry[?]]]
  implicitly[Column[GameTime] <:< Column[CEntry[?]]]

  implicitly[Column[GameTime] with Column[GameDay] <:< Column[CEntry[?]]]

  implicitly[Column[GameTime] | Column[GameDay] <:< Column[CEntry[?]]]

  implicitly[Column[GameTime] with Column[GameDay] <:< Column[GameTime]]
  implicitly[Column[GameTime] with Column[GameDay] <:< Column[GameDay]]
  implicitly[Column[GameAwayTeam] <:< Column[?]]
  implicitly[Column[GameAwayTeam] with Column[GameDay] <:< Column[GameDay]]

  implicitly[Column[GameTime & GameDay] <:< Column[CEntry[?]]]

  // ******************************************************************************************************************/

  def checkFilteredSegment(db: V1Database, columns: Set[String]): Boolean =
    columns.forall(column => db.getFilter(column).ne(null))

  def checkSortedSegment(db: V1Database, columns: Set[String]): Boolean =
    columns.forall(column => db.getSorter(column).ne(null))

  /** In order to declare this index as "safe to use" all fields from `columnsFromSchema` should be presented in
    * `columnsFromIndex`
    */
  def checkIndexAgainstSchema(
      columnsFromIndex: Set[String],
      columnFromSchema: Set[String],
    ): Boolean = columnFromSchema.forall(columnsFromIndex.contains(_))

  def showSchema(
      columnsFromIndex: Set[String]
    ): String =
    def indType(indexType: GamesSchema.IndexType) =
      indexType match
        case IndexType.Filterable      => IndexType.Filterable.name
        case IndexType.Sortable        => IndexType.Sortable.name
        case IndexType.Both            => IndexType.Both.name
        case IndexType.Unrecognized(_) => IndexType.Unrecognized(-1).name

    def fieldType(fieldType: GamesSchema.FieldType): String =
      fieldType match
        case FieldType.Str             => FieldType.Str.name
        case FieldType.Integer         => FieldType.Integer.name
        case FieldType.Dbl             => FieldType.Dbl.name
        case FieldType.Lng             => FieldType.Lng.name
        case FieldType.Bytes           => FieldType.Bytes.name
        case FieldType.Unrecognized(_) => FieldType.Unrecognized(-1).name

    "\n" +
      columnsFromIndex
        .map { name =>
          IndexColumns
            .find { i =>
              i match
                case Pcolumn.Stage(v)    => v.companion.scalaDescriptor.name == name
                case Pcolumn.AwayTeam(v) => v.companion.scalaDescriptor.name == name
                case Pcolumn.HomeTeam(v) => v.companion.scalaDescriptor.name == name
                case Pcolumn.Time(v)     => v.companion.scalaDescriptor.name == name
                case Pcolumn.Winner(v)   => v.companion.scalaDescriptor.name == name
                case Pcolumn.Year(v)     => v.companion.scalaDescriptor.name == name
                case Pcolumn.Month(v)    => v.companion.scalaDescriptor.name == name
                case Pcolumn.Day(v)      => v.companion.scalaDescriptor.name == name
                case Pcolumn.Empty       => false
            // case Index.Fake(v)     ⇒ v.companion.scalaDescriptor.name == name
            }
            .map {
              case Pcolumn.Stage(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.AwayTeam(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.HomeTeam(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Time(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Winner(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Year(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Month(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Day(v) =>
                "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(
                  v.indexType
                ) + "]"
              case Pcolumn.Empty => ""
              // case Index.Fake(v) ⇒ "[" + v.companion.scalaDescriptor.name + ":" + fieldType(v.`type`) + ":" + indType(v.indexType) + "]"
            }
        }
        .flatten
        .mkString("\n")

  def stage(v: String): StageErr | Stage = yoctodb.Stage.validate(v)
  def team(v: String): TeamErr | Team = yoctodb.Team.validate(v)

  def stageE(v: String): Either[StageErr, Stage] = yoctodb.Stage.validateEither(v)
  def teamE(v: String): Either[TeamErr, Team] = yoctodb.Team.validateEither(v)

  def teamV(v: String): Either[String, String] =
    yoctodb.Team.validateWith(v, v => Right(v.toString), err => Left(err.raw + ":" + err.message))

  def validateBoth(): FromPredicate.Aux[String, Stage | Team] =
    yoctodb.Stage.or(yoctodb.Team)

  def runV(
      input: String
    ): FromPredicate.Aux[String, Stage | Team]#Error | FromPredicate.Aux[String, Stage | Team]#Valid =
    validateBoth().validate(input)

  def runEither(
      input: String
    ): Either[FromPredicate.Aux[String, Stage | Team]#Error, FromPredicate.Aux[String, Stage | Team]#Valid] =
    validateBoth().validateEither(input)

  def bothTeams(a: String, b: String): Either[List[TeamErr], (Team, Team)] =
    teamE(a).left.map(err => List(err)) match
      case Left(errors) =>
        teamE(b).fold(err => Left(err :: errors), _ => Left(errors))
      case Right(a) =>
        teamE(b).map(b => (a, b)).left.map(er => List(er))

  def bothStages(a: String, b: String): Either[List[StageErr], (Stage, Stage)] =
    val s = stageE(b)
    stageE(a).left.map(err => List(err)) match
      case Left(errors) => s.fold(err => Left(err :: errors), _ => Left(errors))
      case Right(a)     => s.map(b => (a, b)).left.map(er => List(er))

  def validateSchema(yoctoDb: V1Database): Either[List[?], (FSchema, SSchema)] =
    val s = SSchema.validateEither(yoctoDb)
    FSchema.validateEither(yoctoDb).left.map(err => List(err)) match
      case Left(errors) => s.fold(err => Left(err :: errors), _ => Left(errors))
      case Right(f)     => s.map(s => (f, s)).left.map(er => List(er))

  runEither("season-19-20") match
    case Left(err) => println("Error: " + err)
    case Right(v)  => println(s"out: $v")

  // safe
  team("lal") match
    case err: TeamErr => println("Error: " + err)
    case r            => println(s"out: $r")

// def validateSchema(yoctoDb: V1Database) = FSchema.and(SSchema).validateEither(yoctoDb)

/*: FromPredicate.Aux[V1Database, FSchema | SSchema]#Error | FromPredicate.Aux[V1Database, FSchema | SSchema]#Valid*/
// FSchema.and(SSchema).validate(yoctoDb)
