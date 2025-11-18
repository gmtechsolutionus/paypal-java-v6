const credentialForm = document.getElementById('credential-form');
const paymentForm = document.getElementById('payment-form');
const credentialStatus = document.getElementById('credential-status');
const paymentStatus = document.getElementById('payment-status');

const state = {
    credentialToken: null,
    clientId: null
};

credentialForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = new FormData(credentialForm);
    const payload = {
        clientId: data.get('clientId')?.trim(),
        clientSecret: data.get('clientSecret')?.trim(),
        environment: data.get('environment')
    };
    credentialStatus.textContent = 'Validating with PayPal...';
    try {
        const response = await fetch('/api/credentials/validate', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        const json = await response.json();
        if (!response.ok || !json.valid) {
            throw new Error(json.message || 'Validation failed');
        }
        state.credentialToken = json.credentialToken;
        state.clientId = payload.clientId;
        credentialStatus.textContent = `Credentials accepted (${json.environment}). Token expires in ~12h.`;
        loadPayPalSdk(payload.clientId);
    } catch (error) {
        credentialStatus.textContent = `Validation error: ${error.message}`;
    }
});

paymentForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!state.credentialToken) {
        paymentStatus.textContent = 'Validate credentials first.';
        return;
    }
    const data = new FormData(paymentForm);
    const payload = {
        credentialToken: state.credentialToken,
        amount: data.get('amount'),
        currencyCode: data.get('currencyCode')?.toUpperCase() || 'USD',
        cardholderName: data.get('cardholderName'),
        cardNumber: data.get('cardNumber'),
        expiry: data.get('expiry'),
        securityCode: data.get('securityCode'),
        billingAddress: extractBilling(data)
    };
    paymentStatus.textContent = 'Sending card payment to PayPal...';
    try {
        const response = await fetch('/api/payment/process', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        const json = await response.json();
        if (!response.ok) {
            throw new Error(json.message || 'Payment declined');
        }
        paymentStatus.textContent = `Payment status: ${json.status} (Order ${json.orderId})`;
    } catch (error) {
        paymentStatus.textContent = `Payment failed: ${error.message}`;
    }
});

function extractBilling(data) {
    const billing = {
        addressLine1: data.get('addressLine1'),
        addressLine2: data.get('addressLine2'),
        adminArea1: data.get('adminArea1'),
        adminArea2: data.get('adminArea2'),
        postalCode: data.get('postalCode'),
        countryCode: data.get('countryCode')?.toUpperCase()
    };
    return Object.values(billing).some(Boolean) ? billing : null;
}

function loadPayPalSdk(clientId) {
    const existing = document.querySelector('script[data-paypal-sdk]');
    if (existing) {
        existing.remove();
    }
    const script = document.createElement('script');
    script.src = `https://www.paypal.com/sdk/js?client-id=${encodeURIComponent(clientId)}&components=buttons,funding-eligibility`;
    script.async = true;
    script.dataset.paypalSdk = 'true';
    script.onload = () => console.log('PayPal JS SDK loaded');
    document.body.appendChild(script);
}

