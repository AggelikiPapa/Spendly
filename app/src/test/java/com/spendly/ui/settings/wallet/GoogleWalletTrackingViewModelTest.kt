package com.spendly.ui.settings.wallet

import com.spendly.wallet.listener.NotificationAccessGateway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleWalletTrackingViewModelTest {
    @Test fun grantedAccessIsShownFromSystemGateway() {
        val access = FakeAccess(granted = true)
        val vm = GoogleWalletTrackingViewModel(access)
        assertFalse(vm.uiState.value.isChecking)
        assertTrue(vm.uiState.value.notificationAccessGranted)
        assertEquals(1, access.checks)
    }

    @Test fun missingAccessIsShownFromSystemGateway() {
        val vm = GoogleWalletTrackingViewModel(FakeAccess(granted = false))
        assertFalse(vm.uiState.value.notificationAccessGranted)
        assertNull(vm.uiState.value.error)
    }

    @Test fun resumeRefreshRechecksBothGrantAndRevocation() {
        val access = FakeAccess(granted = false)
        val vm = GoogleWalletTrackingViewModel(access)
        access.granted = true
        vm.refreshAccess() // Invoked by the screen's ON_RESUME lifecycle effect.
        assertTrue(vm.uiState.value.notificationAccessGranted)
        access.granted = false
        vm.refreshAccess()
        assertFalse(vm.uiState.value.notificationAccessGranted)
        assertEquals(3, access.checks)
    }

    @Test fun openingSettingsNeverChangesPermissionStateUntilGatewayIsRechecked() {
        val access = FakeAccess(granted = false)
        val vm = GoogleWalletTrackingViewModel(access)
        vm.openSystemSettings()
        assertEquals(1, access.opens)
        assertFalse(vm.uiState.value.notificationAccessGranted)
        access.granted = true
        vm.refreshAccess()
        assertTrue(vm.uiState.value.notificationAccessGranted)
    }

    @Test fun unavailableSettingsAndCheckFailureShowSafeErrors() {
        val access = FakeAccess(granted = false)
        val vm = GoogleWalletTrackingViewModel(access)
        access.canOpen = false
        vm.openSystemSettings()
        assertNotNull(vm.uiState.value.error)
        access.failCheck = true
        vm.refreshAccess()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isChecking)
    }

    @Test fun aNewViewModelReadsCurrentSystemStateInsteadOfPersistedFlag() {
        val access = FakeAccess(granted = false)
        GoogleWalletTrackingViewModel(access)
        access.granted = true
        val newVm = GoogleWalletTrackingViewModel(access)
        assertTrue(newVm.uiState.value.notificationAccessGranted)
    }

    private class FakeAccess(var granted: Boolean) : NotificationAccessGateway {
        var checks = 0
        var opens = 0
        var canOpen = true
        var failCheck = false
        override fun isAccessGranted(): Boolean {
            checks++
            if (failCheck) throw IllegalStateException("system detail")
            return granted
        }
        override fun openSettings(): Boolean { opens++; return canOpen }
    }
}
