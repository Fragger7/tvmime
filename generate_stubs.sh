mkdir -p tvApp/src/main/java/com/streamvault/app/diagnostics
echo "package com.streamvault.app.diagnostics; class CrashReportStore @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/app/diagnostics/CrashReportStore.kt

mkdir -p tvApp/src/main/java/com/streamvault/app/tv
echo "package com.streamvault.app.tv; class LauncherRecommendationsManager @javax.inject.Inject constructor(); class WatchNextManager @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/app/tv/Stubs.kt

mkdir -p tvApp/src/main/java/com/streamvault/app/tvinput
echo "package com.streamvault.app.tvinput; class TvInputChannelSyncManager @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/app/tvinput/TvInputChannelSyncManager.kt

mkdir -p tvApp/src/main/java/com/streamvault/app/update
echo "package com.streamvault.app.update; class AppUpdateInstaller @javax.inject.Inject constructor(); class GitHubReleaseChecker @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/app/update/Stubs.kt

mkdir -p tvApp/src/main/java/com/streamvault/data/local/dao
echo "package com.streamvault.data.local.dao; class ProgramDao @javax.inject.Inject constructor(); class XtreamIndexJobDao @javax.inject.Inject constructor(); class XtreamLiveOnboardingDao @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/data/local/dao/Stubs.kt

mkdir -p tvApp/src/main/java/com/streamvault/domain/manager
echo "package com.streamvault.domain.manager; class BackupManager @javax.inject.Inject constructor(); class BackupRestoreStatusStore @javax.inject.Inject constructor(); class DriveBackupSyncManager @javax.inject.Inject constructor(); class RecordingManager @javax.inject.Inject constructor(); class ParentalControlManager @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/domain/manager/Stubs.kt

mkdir -p tvApp/src/main/java/com/streamvault/domain/usecase
echo "package com.streamvault.domain.usecase; class ValidateAndAddProvider @javax.inject.Inject constructor(); class ImportBackup @javax.inject.Inject constructor(); class SyncProvider @javax.inject.Inject constructor(); class GetCustomCategories @javax.inject.Inject constructor()" > tvApp/src/main/java/com/streamvault/domain/usecase/Stubs.kt
