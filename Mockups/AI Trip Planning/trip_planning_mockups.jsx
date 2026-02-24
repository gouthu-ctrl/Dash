import React, { useState } from 'react';
import { Sparkles, Plus, MapPin, Clock, DollarSign, Star, ChevronRight, X, Check, Calendar, Search, Wand2, TrendingUp, Users, Heart } from 'lucide-react';

const TripPlanningMockups = () => {
  const [activeScreen, setActiveScreen] = useState('tripDetails');
  const [showAISuggestions, setShowAISuggestions] = useState(false);
  const [showTooltip, setShowTooltip] = useState(true);
  const [selectedSuggestion, setSelectedSuggestion] = useState(null);
  const [addedItems, setAddedItems] = useState([]);

  // Sample data
  const tripData = {
    title: "Tokyo Adventure",
    dates: "Mar 15-22, 2026",
    days: 7,
    travelers: 2
  };

  const aiSuggestions = [
    {
      id: 1,
      type: "Activity",
      name: "TeamLab Borderless",
      description: "Immersive digital art museum",
      time: "10:00 AM",
      duration: "2-3 hours",
      price: "$29/person",
      rating: 4.8,
      popularity: "Trending",
      day: "Day 3"
    },
    {
      id: 2,
      type: "Restaurant",
      name: "Tsukiji Sushi Say",
      description: "Authentic Edo-style sushi",
      time: "12:30 PM",
      duration: "1 hour",
      price: "$45/person",
      rating: 4.9,
      popularity: "Top Rated",
      day: "Day 3"
    },
    {
      id: 3,
      type: "Activity",
      name: "Sensoji Temple & Asakusa",
      description: "Historic temple and shopping street",
      time: "3:00 PM",
      duration: "2 hours",
      price: "Free",
      rating: 4.7,
      popularity: "Must See",
      day: "Day 3"
    }
  ];

  const existingItinerary = [
    {
      id: 101,
      day: "Day 3",
      time: "9:00 AM",
      name: "Breakfast at Hotel",
      type: "Meal"
    },
    {
      id: 102,
      day: "Day 3",
      time: "6:00 PM",
      name: "Shibuya Crossing",
      type: "Activity"
    }
  ];

  // Screen 1: Trip Details with Subtle AI Entry Point
  const TripDetailsScreen = () => (
    <div className="relative w-full max-w-md mx-auto bg-gradient-to-b from-blue-50 to-white rounded-3xl shadow-2xl overflow-hidden" style={{ height: '812px' }}>
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-700 text-white px-6 pt-14 pb-6">
        <div className="text-sm opacity-90 mb-1">{tripData.dates}</div>
        <h1 className="text-2xl font-bold mb-2">{tripData.title}</h1>
        <div className="flex gap-4 text-sm opacity-90">
          <span>{tripData.days} days</span>
          <span>•</span>
          <span>{tripData.travelers} travelers</span>
        </div>
      </div>

      {/* Day Tabs */}
      <div className="flex overflow-x-auto px-4 py-3 bg-white border-b gap-2">
        {[1, 2, 3, 4, 5, 6, 7].map(day => (
          <button
            key={day}
            className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
              day === 3 
                ? 'bg-blue-600 text-white shadow-md' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Day {day}
          </button>
        ))}
      </div>

      {/* Itinerary Items */}
      <div className="px-4 py-4 space-y-3 overflow-y-auto" style={{ height: '480px' }}>
        {existingItinerary.map(item => (
          <div key={item.id} className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100">
            <div className="flex items-start gap-3">
              <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center flex-shrink-0">
                <Clock className="w-6 h-6 text-blue-600" />
              </div>
              <div className="flex-1">
                <div className="text-xs text-gray-500 mb-1">{item.time}</div>
                <div className="font-semibold text-gray-900">{item.name}</div>
                <div className="text-xs text-gray-600 mt-1">{item.type}</div>
              </div>
            </div>
          </div>
        ))}

        {addedItems.map(item => (
          <div key={item.id} className="bg-white rounded-2xl p-4 shadow-sm border-2 border-green-200 animate-pulse-once">
            <div className="flex items-start gap-3">
              <div className="w-12 h-12 rounded-xl bg-green-100 flex items-center justify-center flex-shrink-0">
                <Check className="w-6 h-6 text-green-600" />
              </div>
              <div className="flex-1">
                <div className="text-xs text-gray-500 mb-1">{item.time}</div>
                <div className="font-semibold text-gray-900">{item.name}</div>
                <div className="text-xs text-gray-600 mt-1">{item.type}</div>
              </div>
            </div>
          </div>
        ))}

        {/* Empty State with AI Hint */}
        {existingItinerary.length < 3 && (
          <div className="text-center py-8 px-4">
            <div className="w-16 h-16 rounded-full bg-gray-100 mx-auto mb-4 flex items-center justify-center">
              <MapPin className="w-8 h-8 text-gray-400" />
            </div>
            <p className="text-gray-600 mb-2">Your Day 3 looks empty</p>
            <p className="text-sm text-gray-500">Add activities or get AI suggestions</p>
          </div>
        )}
      </div>

      {/* Bottom Action Bar - Single Primary Action */}
      <div className="absolute bottom-0 left-0 right-0 bg-white border-t px-4 py-4 safe-area-bottom">
        <div className="flex gap-3">
          {/* Secondary: Manual Add */}
          <button className="w-14 h-14 rounded-full bg-gray-100 flex items-center justify-center hover:bg-gray-200 transition-all">
            <Plus className="w-6 h-6 text-gray-700" />
          </button>
          
          {/* Primary: AI Suggestions */}
          <button 
            onClick={() => setShowAISuggestions(true)}
            className="flex-1 h-14 rounded-full bg-gradient-to-r from-blue-600 to-blue-700 text-white font-semibold flex items-center justify-center gap-2 shadow-lg hover:shadow-xl transition-all"
          >
            <Sparkles className="w-5 h-5" />
            Get AI Suggestions
          </button>
        </div>
        
        {/* First-time Tooltip */}
        {showTooltip && (
          <div className="absolute bottom-20 left-20 right-4 bg-gray-900 text-white text-sm p-4 rounded-2xl shadow-xl animate-fade-in">
            <button 
              onClick={() => setShowTooltip(false)}
              className="absolute top-2 right-2 text-white/60 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
            <div className="pr-6">
              <strong>💡 New to AI planning?</strong>
              <p className="mt-1 opacity-90">Tap "Get AI Suggestions" to discover activities, restaurants, and experiences for this day</p>
            </div>
            <div className="w-4 h-4 bg-gray-900 absolute -bottom-2 left-8 transform rotate-45"></div>
          </div>
        )}
      </div>
    </div>
  );

  // Screen 2: AI Suggestions Bottom Sheet
  const AISuggestionsSheet = () => (
    <div className="relative w-full max-w-md mx-auto bg-white rounded-3xl shadow-2xl overflow-hidden" style={{ height: '812px' }}>
      {/* Dimmed Background (Trip Details) */}
      <div className="absolute inset-0 bg-gray-900/40"></div>

      {/* Bottom Sheet */}
      <div className="absolute bottom-0 left-0 right-0 bg-white rounded-t-3xl" style={{ height: '680px' }}>
        {/* Handle */}
        <div className="w-12 h-1.5 bg-gray-300 rounded-full mx-auto mt-3 mb-4"></div>

        {/* Header */}
        <div className="px-6 pb-4 border-b">
          <div className="flex items-center justify-between mb-3">
            <div>
              <h2 className="text-xl font-bold text-gray-900">AI Suggestions</h2>
              <p className="text-sm text-gray-600 mt-0.5">For Day 3 • Mar 17</p>
            </div>
            <button 
              onClick={() => setShowAISuggestions(false)}
              className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center hover:bg-gray-200"
            >
              <X className="w-5 h-5 text-gray-700" />
            </button>
          </div>

          {/* Context Pills */}
          <div className="flex gap-2 overflow-x-auto pb-2">
            <div className="px-3 py-1.5 bg-blue-50 text-blue-700 rounded-full text-xs font-medium whitespace-nowrap flex items-center gap-1">
              <Wand2 className="w-3 h-3" />
              Personalized
            </div>
            <div className="px-3 py-1.5 bg-purple-50 text-purple-700 rounded-full text-xs font-medium whitespace-nowrap flex items-center gap-1">
              <TrendingUp className="w-3 h-3" />
              Popular in Tokyo
            </div>
            <div className="px-3 py-1.5 bg-green-50 text-green-700 rounded-full text-xs font-medium whitespace-nowrap">
              Fits your schedule
            </div>
          </div>
        </div>

        {/* Suggestions List */}
        <div className="overflow-y-auto px-4 py-4 space-y-3" style={{ height: '520px' }}>
          {aiSuggestions.map(suggestion => (
            <div 
              key={suggestion.id}
              className={`bg-white rounded-2xl border-2 transition-all ${
                selectedSuggestion === suggestion.id 
                  ? 'border-blue-500 shadow-lg' 
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              <div className="p-4">
                {/* Header */}
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-semibold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                        {suggestion.type}
                      </span>
                      <span className="text-xs font-medium text-orange-600 bg-orange-50 px-2 py-0.5 rounded flex items-center gap-1">
                        <Star className="w-3 h-3 fill-current" />
                        {suggestion.popularity}
                      </span>
                    </div>
                    <h3 className="font-bold text-gray-900 text-lg">{suggestion.name}</h3>
                    <p className="text-sm text-gray-600 mt-1">{suggestion.description}</p>
                  </div>
                </div>

                {/* Details */}
                <div className="flex flex-wrap gap-3 text-sm text-gray-700 mb-3">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-gray-400" />
                    <span>{suggestion.time}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Calendar className="w-4 h-4 text-gray-400" />
                    <span>{suggestion.duration}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <DollarSign className="w-4 h-4 text-gray-400" />
                    <span>{suggestion.price}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Star className="w-4 h-4 text-yellow-500 fill-current" />
                    <span className="font-medium">{suggestion.rating}</span>
                  </div>
                </div>

                {/* Action Button */}
                <button
                  onClick={() => {
                    setAddedItems([...addedItems, suggestion]);
                    setSelectedSuggestion(suggestion.id);
                    setTimeout(() => {
                      setShowAISuggestions(false);
                      setSelectedSuggestion(null);
                    }, 800);
                  }}
                  className={`w-full h-12 rounded-xl font-semibold transition-all ${
                    addedItems.find(item => item.id === suggestion.id)
                      ? 'bg-green-100 text-green-700 border-2 border-green-300'
                      : selectedSuggestion === suggestion.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-blue-600 text-white hover:bg-blue-700'
                  }`}
                  disabled={addedItems.find(item => item.id === suggestion.id)}
                >
                  {addedItems.find(item => item.id === suggestion.id) ? (
                    <span className="flex items-center justify-center gap-2">
                      <Check className="w-5 h-5" />
                      Added to Day 3
                    </span>
                  ) : (
                    <span className="flex items-center justify-center gap-2">
                      <Plus className="w-5 h-5" />
                      Add to Day 3
                    </span>
                  )}
                </button>
              </div>
            </div>
          ))}

          {/* More suggestions prompt */}
          <button className="w-full py-4 text-blue-600 font-medium text-sm hover:text-blue-700 transition-colors">
            Show more suggestions →
          </button>
        </div>
      </div>
    </div>
  );

  // Screen 3: Alternative - Inline AI Card
  const InlineAIScreen = () => (
    <div className="relative w-full max-w-md mx-auto bg-gradient-to-b from-blue-50 to-white rounded-3xl shadow-2xl overflow-hidden" style={{ height: '812px' }}>
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-700 text-white px-6 pt-14 pb-6">
        <div className="text-sm opacity-90 mb-1">{tripData.dates}</div>
        <h1 className="text-2xl font-bold mb-2">{tripData.title}</h1>
        <div className="flex gap-4 text-sm opacity-90">
          <span>{tripData.days} days</span>
          <span>•</span>
          <span>{tripData.travelers} travelers</span>
        </div>
      </div>

      {/* Day Tabs */}
      <div className="flex overflow-x-auto px-4 py-3 bg-white border-b gap-2">
        {[1, 2, 3, 4, 5, 6, 7].map(day => (
          <button
            key={day}
            className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
              day === 3 
                ? 'bg-blue-600 text-white shadow-md' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Day {day}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="px-4 py-4 space-y-3 overflow-y-auto" style={{ height: '580px' }}>
        {/* First Item */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100">
          <div className="flex items-start gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center flex-shrink-0">
              <Clock className="w-6 h-6 text-blue-600" />
            </div>
            <div className="flex-1">
              <div className="text-xs text-gray-500 mb-1">9:00 AM</div>
              <div className="font-semibold text-gray-900">Breakfast at Hotel</div>
              <div className="text-xs text-gray-600 mt-1">Meal</div>
            </div>
          </div>
        </div>

        {/* AI Suggestion Card - Inline */}
        <div className="bg-gradient-to-br from-blue-50 to-purple-50 rounded-2xl p-4 border-2 border-dashed border-blue-300">
          <div className="flex items-center gap-2 mb-3">
            <Sparkles className="w-5 h-5 text-blue-600" />
            <span className="font-semibold text-gray-900">AI Suggestion</span>
            <span className="ml-auto text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full font-medium">
              Popular
            </span>
          </div>
          
          <h4 className="font-bold text-gray-900 mb-1">TeamLab Borderless</h4>
          <p className="text-sm text-gray-600 mb-3">Immersive digital art museum perfect for your interests</p>
          
          <div className="flex gap-3 text-xs text-gray-600 mb-3">
            <span>⏰ 10:00 AM</span>
            <span>⌛ 2-3 hrs</span>
            <span>💰 $29/person</span>
          </div>

          <div className="flex gap-2">
            <button className="flex-1 h-10 rounded-lg bg-white border border-gray-300 text-gray-700 font-medium hover:bg-gray-50 transition-all">
              Skip
            </button>
            <button className="flex-1 h-10 rounded-lg bg-blue-600 text-white font-semibold hover:bg-blue-700 transition-all flex items-center justify-center gap-1">
              <Plus className="w-4 h-4" />
              Add
            </button>
          </div>
        </div>

        {/* Show more suggestions */}
        <button className="w-full py-3 text-sm text-blue-600 font-medium hover:text-blue-700 transition-colors flex items-center justify-center gap-1">
          <Sparkles className="w-4 h-4" />
          View more AI suggestions
        </button>

        {/* Last item */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100">
          <div className="flex items-start gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center flex-shrink-0">
              <Clock className="w-6 h-6 text-blue-600" />
            </div>
            <div className="flex-1">
              <div className="text-xs text-gray-500 mb-1">6:00 PM</div>
              <div className="font-semibold text-gray-900">Shibuya Crossing</div>
              <div className="text-xs text-gray-600 mt-1">Activity</div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom FAB */}
      <div className="absolute bottom-6 right-6">
        <button className="w-14 h-14 rounded-full bg-blue-600 text-white shadow-lg hover:shadow-xl transition-all flex items-center justify-center">
          <Plus className="w-6 h-6" />
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-100 via-blue-50 to-purple-50 p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-3">Dash Trip Planning - AI Features</h1>
          <p className="text-gray-600 mb-6">Clean, intuitive UI with progressive disclosure of AI capabilities</p>
          
          {/* Screen Selector */}
          <div className="inline-flex gap-2 bg-white p-2 rounded-xl shadow-sm">
            <button
              onClick={() => {
                setActiveScreen('tripDetails');
                setShowAISuggestions(false);
              }}
              className={`px-4 py-2 rounded-lg font-medium transition-all ${
                activeScreen === 'tripDetails' 
                  ? 'bg-blue-600 text-white shadow-md' 
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Option 1: Bottom Sheet
            </button>
            <button
              onClick={() => {
                setActiveScreen('suggestions');
                setShowAISuggestions(true);
              }}
              className={`px-4 py-2 rounded-lg font-medium transition-all ${
                activeScreen === 'suggestions' 
                  ? 'bg-blue-600 text-white shadow-md' 
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Option 2: Bottom Sheet (Active)
            </button>
            <button
              onClick={() => {
                setActiveScreen('inline');
                setShowAISuggestions(false);
              }}
              className={`px-4 py-2 rounded-lg font-medium transition-all ${
                activeScreen === 'inline' 
                  ? 'bg-blue-600 text-white shadow-md' 
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              Option 3: Inline Cards
            </button>
          </div>
        </div>

        {/* Screen Display */}
        <div className="mb-12">
          {activeScreen === 'tripDetails' && !showAISuggestions && <TripDetailsScreen />}
          {(activeScreen === 'suggestions' || showAISuggestions) && <AISuggestionsSheet />}
          {activeScreen === 'inline' && <InlineAIScreen />}
        </div>

        {/* Design Principles */}
        <div className="grid md:grid-cols-3 gap-6 max-w-5xl mx-auto">
          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center mb-4">
              <Sparkles className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="font-bold text-gray-900 mb-2">One Primary Action</h3>
            <p className="text-sm text-gray-600">
              "Get AI Suggestions" is the single primary CTA. Manual add is secondary (smaller, gray button).
            </p>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center mb-4">
              <ChevronRight className="w-6 h-6 text-purple-600" />
            </div>
            <h3 className="font-bold text-gray-900 mb-2">Progressive Disclosure</h3>
            <p className="text-sm text-gray-600">
              AI features hidden until user taps. No auto-suggestions. User controls when they want AI help.
            </p>
          </div>

          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center mb-4">
              <Users className="w-6 h-6 text-green-600" />
            </div>
            <h3 className="font-bold text-gray-900 mb-2">Zero Cognitive Load</h3>
            <p className="text-sm text-gray-600">
              Clear labels, contextual tooltips (first use only), instant feedback when items are added.
            </p>
          </div>
        </div>

        {/* Implementation Notes */}
        <div className="mt-8 bg-white rounded-2xl p-8 shadow-sm max-w-5xl mx-auto">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Implementation Notes</h2>
          
          <div className="space-y-4 text-gray-700">
            <div>
              <h4 className="font-semibold text-gray-900 mb-2">🎯 Recommended Approach: Option 2 (Bottom Sheet)</h4>
              <ul className="list-disc list-inside space-y-1 text-sm ml-4">
                <li>Keeps main screen clean and focused on the itinerary</li>
                <li>AI suggestions appear in a modal bottom sheet on user request</li>
                <li>Easy to browse multiple suggestions without losing context</li>
                <li>One-tap add to itinerary with visual feedback</li>
                <li>Follows Material Design 3 bottom sheet patterns</li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold text-gray-900 mb-2">✨ Key Features</h4>
              <ul className="list-disc list-inside space-y-1 text-sm ml-4">
                <li><strong>Context Pills:</strong> Show why suggestions are relevant (Personalized, Popular, Fits schedule)</li>
                <li><strong>Rich Cards:</strong> Each suggestion shows time, duration, price, rating, and popularity</li>
                <li><strong>Visual Feedback:</strong> Button changes to green checkmark when added</li>
                <li><strong>Smart Filtering:</strong> Suggestions fit into existing schedule gaps</li>
                <li><strong>First-time Tooltip:</strong> Disappears after 2 uses (stored in local app state)</li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold text-gray-900 mb-2">📱 Material Design 3 Compliance</h4>
              <ul className="list-disc list-inside space-y-1 text-sm ml-4">
                <li>Rounded corners (16-24px radius)</li>
                <li>Elevation shadows for depth</li>
                <li>Dynamic color theming (blue primary, gradient accents)</li>
                <li>Large touch targets (48dp minimum)</li>
                <li>Consistent spacing (4px, 8px, 12px, 16px, 24px grid)</li>
                <li>Typography scale (heading, body, caption)</li>
              </ul>
            </div>

            <div>
              <h4 className="font-semibold text-gray-900 mb-2">🚀 User Flow</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm ml-4">
                <li>User views their Day 3 itinerary (mostly empty)</li>
                <li>Sees prominent "Get AI Suggestions" button (with optional first-time tooltip)</li>
                <li>Taps button → Bottom sheet slides up with personalized suggestions</li>
                <li>Browses suggestions with full context (time, price, ratings)</li>
                <li>Taps "Add to Day 3" on desired items → Button animates to green checkmark</li>
                <li>Sheet auto-closes after add → Returns to itinerary with new item visible</li>
                <li>New item has subtle green highlight to show it was just added</li>
              </ol>
            </div>
          </div>
        </div>
      </div>

      <style jsx>{`
        @keyframes fade-in {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .animate-fade-in {
          animation: fade-in 0.3s ease-out;
        }

        @keyframes pulse-once {
          0%, 100% {
            box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
          }
          50% {
            box-shadow: 0 0 0 10px rgba(34, 197, 94, 0);
          }
        }

        .animate-pulse-once {
          animation: pulse-once 1s ease-out;
        }

        .safe-area-bottom {
          padding-bottom: env(safe-area-inset-bottom);
        }
      `}</style>
    </div>
  );
};

export default TripPlanningMockups;
