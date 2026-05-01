name: samsung-contacts
description: Saves a new contact to Samsung Contacts on a Samsung Galaxy device
trigger: when the captured text looks like a business card, name card, or contains a person's name alongside a phone number, email address, or company name
parameters:
  - name: string (full name of the person)
  - phone: string (optional phone number, include country code if present)
  - email: string (optional email address)
  - company: string (optional company or organisation name)
examples:
  - input: "Jane Smith | Acme Corp | jane@acme.com | +1 415 555 0100"
    output: { name: "Jane Smith", company: "Acme Corp", email: "jane@acme.com", phone: "+14155550100" }
  - input: "Dr. Arjun Mehta\nNeurology Dept\n+91 98765 43210"
    output: { name: "Dr. Arjun Mehta", company: "Neurology Dept", phone: "+919876543210" }
