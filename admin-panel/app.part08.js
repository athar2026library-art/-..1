        return;
      }
      await categoriesCol().doc(id).set({
        ...payload,
        createdAt: firebase.firestore.FieldValue.serverTimestamp(),
      });
      toast("تمت إضافة التصنيف");
    }
    closeCategoryEditor();
  }

  async function deleteCategory(id) {
    if (!id || BUILTIN_CATEGORIES.some((b) => b.id === id)) {
      toast("لا يمكن حذف التصنيفات المدمجة");
      return;
    }
    const count = azkar.filter((z) => z.category === id).length;
    if (count > 0) {
      toast(`لا يمكن الحذف: يوجد ${count} ذكر مرتبط`);
      return;
    }
    if (!confirm("حذف هذا التصنيف؟")) return;
    await categoriesCol().doc(id).delete();
    toast("تم حذف التصنيف");
  }

  function listenCategories() {
    if (unsubCategories) unsubCategories();
    unsubCategories = categoriesCol().onSnapshot(
      (snap) => {
        const remote = snap.docs.map((doc) => {
          const d = doc.data() || {};
          return { id: doc.id, ...d };
        });
        mergeCategories(remote);
        renderCategories();
        renderTable();
      },
      (err) => {
        console.warn("categories listen", err);
        mergeCategories([]);
        renderCategories();
      }
    );
