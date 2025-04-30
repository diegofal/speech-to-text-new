import React from 'react';
import { StyleSheet, View, ScrollView, Text } from 'react-native';
import { Divider } from 'react-native-paper';

const TranscriptionList = ({ transcripts }) => {
  if (!transcripts || transcripts.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>
          No transcriptions yet. Start recording to see your transcriptions here.
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.sectionTitle}>Transcriptions</Text>
      <Divider style={styles.divider} />
      <ScrollView style={styles.scrollContainer}>
        {transcripts
          .slice()
          .reverse()
          .map((item) => (
            <View key={item.id} style={styles.transcriptItem}>
              <Text style={styles.transcriptDate}>
                {new Date(item.date).toLocaleString()}
              </Text>
              <Text style={styles.transcriptText}>{item.text}</Text>
            </View>
          ))}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  divider: {
    height: 1,
    backgroundColor: '#ddd',
    marginVertical: 10,
  },
  scrollContainer: {
    flex: 1,
    padding: 10,
  },
  transcriptItem: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 15,
    marginBottom: 10,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 1.41,
  },
  transcriptDate: {
    fontSize: 12,
    color: '#888',
    marginBottom: 5,
  },
  transcriptText: {
    fontSize: 16,
  },
});

export default TranscriptionList;
