import CallKit
import Foundation
import React

@objc(KimoxCallDirectoryModule)
final class KimoxCallDirectoryModule: NSObject {
    @objc static func requiresMainQueueSetup() -> Bool { false }

    @objc(sync:labels:resolver:rejecter:)
    func sync(
        blocked: [String],
        labels: [String: String],
        resolve: @escaping RCTPromiseResolveBlock,
        reject: @escaping RCTPromiseRejectBlock
    ) {
        do {
            try KimoxCallDirectoryStore.save(blocked: blocked, identified: labels)
            resolve(true)
        } catch {
            reject("KIMOX_CALL_DIRECTORY_SYNC", error.localizedDescription, error)
        }
    }

    @objc(status:rejecter:)
    func status(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        CXCallDirectoryManager.sharedInstance.getEnabledStatusForExtension(
            withIdentifier: KimoxCallDirectoryStore.extensionIdentifier
        ) { status, error in
            if let error {
                reject("KIMOX_CALL_DIRECTORY_STATUS", error.localizedDescription, error)
                return
            }
            let value: String
            switch status {
            case .enabled: value = "enabled"
            case .disabled: value = "disabled"
            case .unknown: value = "unknown"
            @unknown default: value = "unknown"
            }
            let defaults = UserDefaults(suiteName: KimoxCallDirectoryStore.appGroup)
            resolve([
                "status": value,
                "lastSyncAt": defaults?.double(forKey: "kimox.callDirectory.lastSyncAt") ?? 0,
                "lastError": defaults?.string(forKey: "kimox.callDirectory.lastError") ?? "",
            ])
        }
    }

    @objc(entries:rejecter:)
    func entries(resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        do {
            let entries = try KimoxCallDirectoryStore.load()
            let labels = Dictionary(uniqueKeysWithValues: entries.identified.map { (String($0.number), $0.label) })
            resolve([
                "blocked": entries.blocked.map(String.init),
                "labels": labels,
            ])
        } catch {
            reject("KIMOX_CALL_DIRECTORY_ENTRIES", error.localizedDescription, error)
        }
    }
}
