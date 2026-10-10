import type { NotificationCopyByLocale } from "./shared";

export function localizedNotificationCopy(type: string): NotificationCopyByLocale {
  switch (type) {
  case "INTEREST":
    return {
      en: { title: "New interest", body: "Someone is interested in your profile. Open the app to view it." },
      te: { title: "కొత్త ఆసక్తి", body: "ఎవరైనా మీ ప్రొఫైల్‌పై ఆసక్తి చూపించారు. చూడటానికి యాప్‌ను తెరవండి." },
      hi: { title: "नई रुचि", body: "किसी ने आपकी प्रोफ़ाइल में रुचि दिखाई है। देखने के लिए ऐप खोलें।" },
    };
  case "MATCH":
    return {
      en: { title: "New mutual match", body: "You have a new mutual match. Open the app to view the profile." },
      te: { title: "కొత్త పరస్పర మ్యాచ్", body: "మీకు కొత్త పరస్పర మ్యాచ్ వచ్చింది. ప్రొఫైల్ చూడటానికి యాప్‌ను తెరవండి." },
      hi: { title: "नया पारस्परिक मैच", body: "आपका नया पारस्परिक मैच हुआ है। प्रोफ़ाइल देखने के लिए ऐप खोलें।" },
    };
  case "MESSAGE":
    return {
      en: { title: "New message", body: "Open the app to view your message." },
      te: { title: "కొత్త సందేశం", body: "మీ సందేశాన్ని చూడటానికి యాప్‌ను తెరవండి." },
      hi: { title: "नया संदेश", body: "अपना संदेश देखने के लिए ऐप खोलें।" },
    };
  case "CALL_REQUEST":
  case "CALL_ACCEPTED":
  case "CALL_DECLINED":
  case "CALL_CANCELLED":
    return {
      en: { title: "Secure call request update", body: "Open Matree to review the secure-call request." },
      te: { title: "సురక్షిత కాల్ అభ్యర్థన నవీకరణ", body: "సురక్షిత కాల్ అభ్యర్థనను చూడటానికి Matree తెరవండి." },
      hi: { title: "सुरक्षित कॉल अनुरोध अपडेट", body: "सुरक्षित कॉल अनुरोध देखने के लिए Matree खोलें।" },
    };
  case "PHOTO_REQUEST":
    return {
      en: { title: "Photo request", body: "A member requested a profile photo. Open Matree to review it." },
      te: { title: "ఫోటో అభ్యర్థన", body: "ఒక సభ్యుడు ప్రొఫైల్ ఫోటో కోరారు. చూడటానికి Matree తెరవండి." },
      hi: { title: "फ़ोटो अनुरोध", body: "एक सदस्य ने प्रोफ़ाइल फ़ोटो का अनुरोध किया है। देखने के लिए Matree खोलें।" },
    };
  case "PHOTO_ACCESS_REQUEST":
    return {
      en: { title: "Photo access request", body: "Open Privacy & visibility in Matree to review the photo access request." },
      te: { title: "ఫోటో యాక్సెస్ అభ్యర్థన", body: "ఫోటో యాక్సెస్ అభ్యర్థనను చూడటానికి Matreeలో గోప్యత సెట్టింగ్‌లను తెరవండి." },
      hi: { title: "फ़ोटो पहुँच अनुरोध", body: "फ़ोटो पहुँच अनुरोध देखने के लिए Matree की गोपनीयता सेटिंग खोलें।" },
    };
  default:
    return {};
  }
}

