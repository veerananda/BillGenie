package com.billgenie.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "subscription")
data class Subscription(
    @PrimaryKey
    val id: Int = 1, // Single subscription per installation
    val planType: SubscriptionPlan,
    val status: SubscriptionStatus,
    val startDate: Date,
    val endDate: Date,
    val trialEndDate: Date? = null,
    val isTrialActive: Boolean = false,
    val maxLocations: Int,
    val maxUsers: Int,
    val features: List<String>, // JSON array of enabled features
    val lastPaymentDate: Date? = null,
    val nextBillingDate: Date? = null
)

enum class SubscriptionPlan(val displayName: String, val monthlyPrice: Double) {
    FREE("Free", 0.0),
    BASIC("Basic", 29.0),
    PROFESSIONAL("Professional", 79.0),
    ENTERPRISE("Enterprise", 199.0)
}

enum class SubscriptionStatus {
    TRIAL,
    ACTIVE,
    EXPIRED,
    CANCELLED,
    PAYMENT_FAILED
}

data class PlanFeatures(
    val planType: SubscriptionPlan,
    val maxLocations: Int,
    val maxUsers: Int,
    val features: List<String>
) {
    companion object {
        fun getFeatures(plan: SubscriptionPlan): PlanFeatures {
            return when (plan) {
                SubscriptionPlan.FREE -> PlanFeatures(
                    planType = plan,
                    maxLocations = 1,
                    maxUsers = 3,
                    features = listOf("basic_orders", "simple_inventory")
                )
                SubscriptionPlan.BASIC -> PlanFeatures(
                    planType = plan,
                    maxLocations = 1,
                    maxUsers = 5,
                    features = listOf("basic_orders", "inventory_management", "basic_reports", "sales_tracking")
                )
                SubscriptionPlan.PROFESSIONAL -> PlanFeatures(
                    planType = plan,
                    maxLocations = 3,
                    maxUsers = -1, // Unlimited
                    features = listOf("all_basic", "advanced_analytics", "cloud_backup", "multi_location", "priority_support")
                )
                SubscriptionPlan.ENTERPRISE -> PlanFeatures(
                    planType = plan,
                    maxLocations = -1, // Unlimited
                    maxUsers = -1, // Unlimited
                    features = listOf("all_features", "custom_integrations", "white_label", "dedicated_support", "api_access")
                )
            }
        }
    }
}