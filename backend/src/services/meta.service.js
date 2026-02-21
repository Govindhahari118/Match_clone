const FILTER_META = {
    searchModes: ["regular", "advanced", "keyword", "profile_id"],
    gender: ["Bride (Woman)", "Groom (Man)"],
    religion: ["Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi"],
    caste: ["Brahmin", "Kshatriya", "Vaishya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat", "Maratha", "Yadav", "Nair"],
    motherTongue: ["Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"],
    maritalStatus: ["Never Married", "Divorced", "Widowed", "Awaiting Divorce", "Separated", "Annulled"],
    education: ["10th", "12th", "Diploma", "Graduate", "Post Graduate", "Doctorate", "Professional"],
    profession: ["Engineer", "Doctor", "CA/Finance", "Govt/PSU", "Lawyer", "Teacher", "Business", "NRI", "Designer", "Software Engineer", "Architect", "Other"],
    income: ["Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"],
    bodyType: ["Slim", "Average", "Athletic", "Heavy"],
    photoSettings: ["Public", "Protected", "Request Access"],
    residentialStatus: ["Citizen", "Permanent Resident", "Work Permit", "Student Visa", "Other"],
    children: ["No", "Yes, living with me", "Yes, not living with me"],
    premiumFilters: ["Online Now", "Horoscope Compatibility", "Verification Level", "Last Active"],
};

const LOCATION_META = {
    countries: [
        {
            code: "IN",
            name: "India",
            states: [
                {
                    name: "Maharashtra",
                    cities: ["Mumbai", "Pune", "Nagpur", "Nashik"],
                    districts: ["Mumbai City", "Mumbai Suburban", "Pune", "Nagpur", "Nashik"],
                },
                {
                    name: "Karnataka",
                    cities: ["Bangalore", "Mysore", "Mangalore", "Hubli"],
                    districts: ["Bangalore Urban", "Bangalore Rural", "Mysore", "Dakshina Kannada"],
                },
                {
                    name: "Tamil Nadu",
                    cities: ["Chennai", "Coimbatore", "Madurai", "Trichy"],
                    districts: ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli"],
                },
                {
                    name: "Telangana",
                    cities: ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
                    districts: ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
                },
                {
                    name: "Delhi",
                    cities: ["New Delhi", "Delhi"],
                    districts: ["Central Delhi", "South Delhi", "North Delhi", "West Delhi", "East Delhi"],
                },
            ],
        },
    ],
};

const COMMUNITY_META = {
    religions: [
        { name: "Hindu", castes: ["Brahmin", "Kshatriya", "Vaishya", "Kayastha", "Rajput", "Reddy", "Naidu", "Maratha", "Yadav", "Nair"] },
        { name: "Muslim", castes: ["Sunni", "Shia", "Syed", "Pathan", "Sheikh", "Mughal"] },
        { name: "Christian", castes: ["Roman Catholic", "Protestant", "CSI", "Marthoma"] },
        { name: "Sikh", castes: ["Jat", "Khatri", "Arora", "Ramgarhia"] },
        { name: "Jain", castes: ["Digambar", "Shwetambar"] },
        { name: "Buddhist", castes: ["Navayana", "Mahayana", "Theravada"] },
    ],
    motherTongue: FILTER_META.motherTongue,
};

function withAny(options) {
    return ["Any", ...options];
}

const metaService = {
    getFilterMeta() {
        return {
            ...FILTER_META,
            religion: withAny(FILTER_META.religion),
            caste: withAny(FILTER_META.caste),
            motherTongue: withAny(FILTER_META.motherTongue),
            maritalStatus: withAny(FILTER_META.maritalStatus),
            education: withAny(FILTER_META.education),
            profession: withAny(FILTER_META.profession),
            income: withAny(FILTER_META.income),
            bodyType: withAny(FILTER_META.bodyType),
            residentialStatus: withAny(FILTER_META.residentialStatus),
            children: withAny(FILTER_META.children),
        };
    },

    getLocationMeta() {
        return LOCATION_META;
    },

    getCommunityMeta() {
        return COMMUNITY_META;
    },
};

module.exports = metaService;
