import AuthenticationServices
import CryptoKit
import Foundation
import Shared
import UIKit

/// Sign in with Apple for the shared `SocialSignIn` seam. Apple signs a hash of a fresh nonce
/// into the ID token; Firebase checks the raw nonce against it, so a token can't be replayed.
final class AppleSignIn: NSObject, IosAppleSignIn {
    private var onCredential: ((String, String) -> Void)?
    private var onCancel: (() -> Void)?
    private var onError: ((String) -> Void)?
    private var rawNonce = ""

    func signIn(
        onCredential: @escaping (String, String) -> Void,
        onCancel: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        self.onCredential = onCredential
        self.onCancel = onCancel
        self.onError = onError
        rawNonce = Self.randomNonce()

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email, .fullName]
        request.nonce = Self.sha256(rawNonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    private func finish(token: String?, error: Error? = nil) {
        if let token {
            onCredential?(token, rawNonce)
        } else if let error, (error as? ASAuthorizationError)?.code != .canceled {
            // The system's own text ("error 1000") means nothing to a user; the usual cause is a
            // device not signed in to an Apple Account.
            NSLog("Sign in with Apple failed: %@", String(describing: error))
            onError?("Apple sign-in didn't work. Check this device is signed in to an Apple Account in Settings, then try again.")
        } else {
            onCancel?()
        }
        onCredential = nil
        onCancel = nil
        onError = nil
    }

    private static func randomNonce(length: Int = 32) -> String {
        let charset = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var bytes = [UInt8](repeating: 0, count: length)
        if SecRandomCopyBytes(kSecRandomDefault, length, &bytes) != errSecSuccess {
            return UUID().uuidString
        }
        return String(bytes.map { charset[Int($0) % charset.count] })
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension AppleSignIn: ASAuthorizationControllerDelegate {
    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        let credential = authorization.credential as? ASAuthorizationAppleIDCredential
        finish(token: credential?.identityToken.flatMap { String(data: $0, encoding: .utf8) })
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        finish(token: nil, error: error)
    }
}

extension AppleSignIn: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}
