import CallKit
import Foundation

final class KimoxCallDirectoryProvider: CXCallDirectoryProvider {
    private let suiteName = "group.com.kimoxcallsecurity.shared"
    private let entriesKey = "kimox.callDirectory.entries.v1"

    override func beginRequest(with context: CXCallDirectoryExtensionContext) {
        context.delegate = self
        let entries = loadEntries()

        for number in entries.blocked.sorted() {
            context.addBlockingEntry(withNextSequentialPhoneNumber: number)
        }
        for entry in entries.identified.sorted(by: { $0.number < $1.number }) {
            context.addIdentificationEntry(
                withNextSequentialPhoneNumber: entry.number,
                label: entry.label
            )
        }
        context.completeRequest()
    }

    private func loadEntries() -> CallDirectoryEntries {
        guard let defaults = UserDefaults(suiteName: suiteName),
              let data = defaults.data(forKey: entriesKey),
              let entries = try? JSONDecoder().decode(CallDirectoryEntries.self, from: data) else {
            return CallDirectoryEntries(blocked: [], identified: [])
        }
        return entries.sanitized
    }
}

extension KimoxCallDirectoryProvider: CXCallDirectoryExtensionContextDelegate {
    func requestFailed(for extensionContext: CXCallDirectoryExtensionContext, withError error: Error) {
        UserDefaults(suiteName: suiteName)?.set(error.localizedDescription, forKey: "kimox.callDirectory.lastError")
    }
}
