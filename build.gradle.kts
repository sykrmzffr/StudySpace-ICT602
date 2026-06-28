// Top-level build file for StudySpace project
// This is where you add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Google services plugin for Firebase integration (T04, T05, T06)
    alias(libs.plugins.google.services) apply false
}
